/**
 * 
 */
package com.acertainbookstore.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * 
 * ReplicationAwareStockManagerHTTPProxy implements the client level synchronous
 * CertainBookStore API declared in the BookStore class. It keeps retrying the
 * API until a consistent reply is returned from the replicas.
 * 
 */
public class ReplicationAwareStockManagerHTTPProxy implements StockManager {
	private HttpClient client;
	private Set<String> slaveAddresses;
	private String masterAddress;
	private String filePath = "/universe/acertainbookstore/proxy.properties";
	private long snapshotId = 0;

	/**
	 * Initialize the client object
	 */
	public ReplicationAwareStockManagerHTTPProxy() throws Exception {
		initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		
		// max concurrent connections to every address 
		client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS);
		
		//max threads
		client.setThreadPool(new QueuedThreadPool(
				BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS)); 
		
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
		if (!this.masterAddress.endsWith("/stock")) {
			this.masterAddress = new String(this.masterAddress + "/stock");
		}

		String slaveAddresses = props.getProperty(BookStoreConstants.KEY_SLAVE);
		for (String slave : slaveAddresses
				.split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
			if (!slave.toLowerCase().startsWith("http://")) {
				slave = new String("http://" + slave);
			}
			if (!slave.endsWith("/stock")) {
				slave = new String(slave + "/stock");
			}

			this.slaveAddresses.add(slave);
		}
	}

	public String getReplicaAddress() {
		return ""; // TODO
	}

	public String getMasterServerAddress() {
		return masterAddress;
	}

	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		ContentExchange exchange = new ContentExchange();
		String urlString;
		urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.ADDBOOKS;
		exchange.setMethod("POST");
		exchange.setURL(urlString);

		String listBooksxmlString = BookStoreUtility
				.serializeObjectToXMLString(bookSet);
		Buffer requestContent = new ByteArrayBuffer(listBooksxmlString);
		exchange.setRequestContent(requestContent);

		BookStoreResult result = null;
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {

		String listBookCopiesxmlString = BookStoreUtility
				.serializeObjectToXMLString(bookCopiesSet);
		Buffer requestContent = new ByteArrayBuffer(listBookCopiesxmlString);
		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.ADDCOPIES;
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	@SuppressWarnings("unchecked")
	public List<StockBook> getBooks() throws BookStoreException {
		BookStoreResult result = null;
		do {
			ContentExchange exchange = new ContentExchange();
			String urlString = getReplicaAddress() + "/"
					+ BookStoreMessageTag.LISTBOOKS;

			exchange.setURL(urlString);
			result = BookStoreUtility.SendAndRecv(this.client, exchange);
		} while (result.getSnapshotId() < this.getSnapshotId());
		this.setSnapshotId(result.getSnapshotId());
		return (List<StockBook>) result.getResultList();
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicksValues)
			throws BookStoreException {

		String xmlStringEditorPicksValues = BookStoreUtility
				.serializeObjectToXMLString(editorPicksValues);
		Buffer requestContent = new ByteArrayBuffer(xmlStringEditorPicksValues);

		BookStoreResult result = null;
		ContentExchange exchange = new ContentExchange();

		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.UPDATEEDITORPICKS + "?";
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	public long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(long snapshotId) {
		this.snapshotId = snapshotId;
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
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public void removeAllBooks() throws BookStoreException {
		BookStoreResult result = null;
		ContentExchange exchange = new ContentExchange();
		String urlString;
		urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.REMOVEALLBOOKS;

		String test = "test";
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		Buffer requestContent = new ByteArrayBuffer(test);
		exchange.setRequestContent(requestContent);

		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		BookStoreResult result = null;
		ContentExchange exchange = new ContentExchange();
		String urlString;
		urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.REMOVEBOOKS;

		String listBooksxmlString = BookStoreUtility
				.serializeObjectToXMLString(isbnSet);
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		Buffer requestContent = new ByteArrayBuffer(listBooksxmlString);
		exchange.setRequestContent(requestContent);

		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());

	}

	@SuppressWarnings("unchecked")
	public List<StockBook> getBooksByISBN(Set<Integer> isbns)
			throws BookStoreException {
		BookStoreResult result = null;
		do {
			ContentExchange exchange = new ContentExchange();
			String urlString = getReplicaAddress() + "/"
					+ BookStoreMessageTag.GETSTOCKBOOKSBYISBN;
			exchange.setMethod("POST");
			exchange.setURL(urlString);

			String listBooksxmlString = BookStoreUtility
					.serializeObjectToXMLString(isbns);
			Buffer requestContent = new ByteArrayBuffer(listBooksxmlString);
			exchange.setRequestContent(requestContent);

			result = BookStoreUtility.SendAndRecv(this.client, exchange);
		} while (result.getSnapshotId() < this.getSnapshotId());
		this.setSnapshotId(result.getSnapshotId());
		return (List<StockBook>) result.getResultList();
	}
}
