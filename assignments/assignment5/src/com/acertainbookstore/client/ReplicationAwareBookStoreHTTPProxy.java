/**
 *
 */
package com.acertainbookstore.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.NetworkException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 *
 * ReplicationAwareBookStoreHTTPProxy implements the client level synchronous
 * CertainBookStore API declared in the BookStore class. It keeps retrying the
 * API until a consistent reply is returned from the replicas
 *
 */
public class ReplicationAwareBookStoreHTTPProxy implements BookStore {
	private HttpClient client;
	private Set<String> slaveAddresses;
	private String masterAddress;
	private String filePath = "proxy.properties";
	private volatile long snapshotId = 0;

	public long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(long snapShotId) {
		this.snapshotId = snapShotId;
	}

	/**
	 * Initialize the client object
	 */
	public ReplicationAwareBookStoreHTTPProxy() throws Exception {
		initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

		// max concurrent connections to every address
		client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS);

		// max threads
		client.setThreadPool(new QueuedThreadPool(BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS));

		// seconds to timeout if no server reply, the request expires
		client.setTimeout(BookStoreClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS);
		client.start();
	}

	private void initializeReplicationAwareMappings() throws IOException {

		Properties props = new Properties();
		slaveAddresses = new HashSet<String>();

		props.load(new FileInputStream(filePath));
		this.masterAddress = props.getProperty(BookStoreConstants.KEY_MASTER);
		if (!this.masterAddress.toLowerCase().startsWith("http://")) {
			this.masterAddress = new String("http://" + this.masterAddress);
		}

		String slaveAddresses = props.getProperty(BookStoreConstants.KEY_SLAVE);
		for (String slave : slaveAddresses.split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
			if (!slave.toLowerCase().startsWith("http://")) {
				slave = new String("http://" + slave);
			}
			this.slaveAddresses.add(slave);
		}

	}

	private void markReplicaServerFaulty(String address) {
		slaveAddresses.remove(address);
	}

	private BookStoreResult sendToAvailableReplica(ContentExchange exchange,
		BookStoreMessageTag tag, String arg) throws BookStoreException {

		BookStoreResult result = null;
        boolean masterIsUp = true;
		while (slaveAddresses.size() > 0 || masterIsUp) {
            // if it's a write operation, we might as well send it directly to master
			String replicaAddress = (BookStoreUtility.isWriteOperation(tag)) ? getMasterServerAddress() : getReplicaAddress();
			String urlString = replicaAddress + "/" + tag + arg;

            ContentExchange exchangeTry = new ContentExchange();
            exchangeTry.setMethod(exchange.getMethod());
            exchangeTry.setRequestContent(exchange.getRequestContent());
			exchangeTry.setURL(urlString);

			try {
                exchange.reset();
				result = BookStoreUtility.SendAndRecv(this.client, exchangeTry);
			} catch (NetworkException ex) {
				// Server dead
				if (slaveAddresses.contains(replicaAddress)) {
                    markReplicaServerFaulty(replicaAddress);
                }
                
                if (replicaAddress == getMasterServerAddress()) {
                    masterIsUp = false;
                    if (BookStoreUtility.isWriteOperation(tag)) {
		                throw new BookStoreException("Master server" + BookStoreConstants.NOT_AVAILABLE);
                    }
                }
			}
            
			if (result != null) {
				return result;
			}
		}

		// No servers was able to process the request.
		throw new BookStoreException("Service" + BookStoreConstants.NOT_AVAILABLE);
	}

	public String getReplicaAddress() {
        int num = (new Random()).nextInt(slaveAddresses.size() + 1);

        // if the generator did not overshoot the size
        if (num < slaveAddresses.size()) {
            int i = 0;
            for (String slave : slaveAddresses) {
                if (i == num) { return slave; }
                ++i;
            }
        }

		return getMasterServerAddress();
	}

	public String getMasterServerAddress() {
		return this.masterAddress;
	}

	public void buyBooks(Set<BookCopy> isbnSet) throws BookStoreException {

		String listISBNsxmlString = BookStoreUtility.serializeObjectToXMLString(isbnSet);
		Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);

		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		exchange.setRequestContent(requestContent);
		
        result = sendToAvailableReplica(exchange, BookStoreMessageTag.BUYBOOKS, "");
		this.setSnapshotId(result.getSnapshotId());
	}

	@SuppressWarnings("unchecked")
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {

		String listISBNsxmlString = BookStoreUtility.serializeObjectToXMLString(isbnSet);
		Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);

		BookStoreResult result = null;
		do {
			ContentExchange exchange = new ContentExchange();
			exchange.setMethod("POST");

			exchange.setRequestContent(requestContent);
			result = sendToAvailableReplica(exchange, BookStoreMessageTag.GETBOOKS, "");
		} while (result.getSnapshotId() < this.getSnapshotId());
		this.setSnapshotId(result.getSnapshotId());
		return (List<Book>) result.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		ContentExchange exchange = new ContentExchange();
		String urlEncodedNumBooks = null;

		try {
			urlEncodedNumBooks = URLEncoder.encode(Integer.toString(numBooks), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new BookStoreException("unsupported encoding of numbooks", ex);
		}

		BookStoreResult result = null;
		do {
			result = sendToAvailableReplica(exchange, BookStoreMessageTag.EDITORPICKS, "?"
					+ BookStoreConstants.BOOK_NUM_PARAM + "=" + urlEncodedNumBooks);
		} while (result.getSnapshotId() < this.getSnapshotId());
		this.setSnapshotId(result.getSnapshotId());

		return (List<Book>) result.getResultList();
	}

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException("Not implemented");
	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException("Not implemented");
	}

}
