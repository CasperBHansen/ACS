package com.acertainbookstore.business;

import java.util.concurrent.Callable;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.NetworkException;
import com.acertainbookstore.utils.BookStoreException;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements Callable<ReplicationResult> {

	private Replication replicationClient = null;
	private ReplicationRequest request = null;
	private String destination = null;


	public CertainBookStoreReplicationTask(Replication replicationClient, ReplicationRequest request, String destination) {
		this.replicationClient = replicationClient;
		this.request = request;
		this.destination = destination;
	}

	@Override
	public ReplicationResult call() {

		boolean successFlag = true;

		try {
			replicationClient.replicate(request);
		} catch (Exception ex) {
			successFlag = false;
		}

		return new ReplicationResult(destination, successFlag); // String serverAddress, boolean replicationSuccessful
	}

}
