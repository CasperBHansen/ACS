package com.acertainbookstore.business;

import java.util.concurrent.Callable;
import com.acertainbookstore.interfaces.Replication;
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
		} catch (BookStoreException ex) {
			System.out.println("E: CertainBookStoreReplicationTask::call: Got exception while replicating: " + ex.getMessage());
			successFlag = false;
		}

		// NOTE: Are we even allowed to do dis shit???
		return new ReplicationResult(destination, successFlag); // String serverAddress, boolean replicationSuccessful

		// TODO Implement this method to invoke the replicate method and flag
		// errors using replicationSuccessful flag in ReplicationResult if any
		// during replication
	}

}
