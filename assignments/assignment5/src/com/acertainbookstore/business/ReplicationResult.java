package com.acertainbookstore.business;

/**
 * ReplicationResult represents the result of a replication.
 */
public class ReplicationResult {
	private String serverAddress; //the server where the replication request was sent
	private boolean replicationSuccessful;

	public ReplicationResult(String serverAddress, boolean replicationSuccessful) {
		this.setServerAddress(serverAddress);
		this.setReplicationSuccessful(replicationSuccessful);
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public boolean isReplicationSuccessful() {
		return replicationSuccessful;
	}

	public void setReplicationSuccessful(boolean replicationSuccessful) {
		this.replicationSuccessful = replicationSuccessful;
	}

}
