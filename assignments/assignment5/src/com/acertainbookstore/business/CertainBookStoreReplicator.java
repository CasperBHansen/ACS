package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.interfaces.Replicator;
import com.acertainbookstore.server.ReplicationAwareServerHTTPProxy;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {
	private Map<String, Replication> replicationClients = null;
	private ExecutorService replicatorThreadPool = null;

	public CertainBookStoreReplicator(int maxReplicatorThreads, Set<String> slaveServers) {
		if(slaveServers == null) {
			return;
		}

		replicationClients = new HashMap<String,Replication>();

		//Create the proxies for each destination slave
		for(String aSlaveServer : slaveServers) {
			replicationClients.put(aSlaveServer, new ReplicationAwareServerHTTPProxy(aSlaveServer));
		}

		//Create the threadpool for concurrently invoking replicate rpcs
		replicatorThreadPool = Executors.newFixedThreadPool(maxReplicatorThreads);
	}

	public List<Future<ReplicationResult>> replicate(ReplicationRequest request) {

		List<Future<ReplicationResult>> resultList = new ArrayList<Future<ReplicationResult>>();

		for (String destination : replicationClients.keySet()) {
			Replication replication = replicationClients.get(destination);
			resultList.add(replicatorThreadPool.submit(new CertainBookStoreReplicationTask(replication, request, new String(destination))));
		}

		return resultList;
	}

	public void markServersFaulty(Set<String> faultyServers) {
		if(faultyServers != null) {
			for(String aFaultyServer : faultyServers) {
				((ReplicationAwareServerHTTPProxy)replicationClients.get(aFaultyServer)).stop();
				replicationClients.remove(aFaultyServer);
			}
		}
	}

	public void finalize() {
		//Shutdown the executor service, invoked when the object is out of scope (garbage collected)
		replicatorThreadPool.shutdownNow();
	}

}
