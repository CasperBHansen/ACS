/**
 *
 */
package com.acertainbookstore.server;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.business.ReplicationRequest;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;
import com.acertainbookstore.client.BookStoreClientConstants;


/**
 * ReplicationAwareServerHTTPProxy implements the client side code for replicate
 * rpc, invoked by the master bookstore to propagate updates to slaves, there is
 * one proxy for each destination slave server
 *
 * @author bonii
 * @author casper
 * @author hans
 *
 */
public class ReplicationAwareServerHTTPProxy implements Replication {
	private HttpClient client;
	private String destinationServerAddress = null;

	/**
	 *
	 */
	public ReplicationAwareServerHTTPProxy(String destinationServerAddress) throws Exception {
		this.destinationServerAddress = destinationServerAddress;

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

	/*
	 *
	 */
	@Override
	public void replicate(ReplicationRequest req) throws BookStoreException {

		String replicatexmlString = BookStoreUtility
				.serializeObjectToXMLString(req);
		Buffer requestContent = new ByteArrayBuffer(replicatexmlString);

		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		String urlString = destinationServerAddress + BookStoreMessageTag.REPLICATE;
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
	}

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
