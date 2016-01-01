package com.acertainbookstore.business;

import java.util.concurrent.Callable;
import com.acertainbookstore.interfaces.Replication;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements Callable<ReplicationResult> {

	public CertainBookStoreReplicationTask(Replication replicationClient, ReplicationRequest request) {
		// TODO Implement this constructor
	}

	@Override
	public ReplicationResult call() {
		// TODO Implement this method to invoke the replicate method and flag
		// errors using replicationSuccessful flag in ReplicationResult if any
		// during replication
		return null;
	}

}
