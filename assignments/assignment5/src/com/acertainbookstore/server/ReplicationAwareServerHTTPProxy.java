/**
 * 
 */
package com.acertainbookstore.server;

import com.acertainbookstore.business.ReplicationRequest;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.BookStoreException;

/**
 * ReplicationAwareServerHTTPProxy implements the client side code for replicate
 * rpc, invoked by the master bookstore to propagate updates to slaves, there is
 * one proxy for each destination slave server
 * 
 * @author bonii
 * 
 */
public class ReplicationAwareServerHTTPProxy implements Replication {
	private String destinationServerAddress = null;

	/**
	 * 
	 */
	public ReplicationAwareServerHTTPProxy(String destinationServerAddress) {
		this.destinationServerAddress = destinationServerAddress;
		// Initialize the http client
	}

	/* 
	 * 
	 */
	@Override
	public void replicate(ReplicationRequest req) throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public void stop() {
		// Shutdown the client
	}

}
