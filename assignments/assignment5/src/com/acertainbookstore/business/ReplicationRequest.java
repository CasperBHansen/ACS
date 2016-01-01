package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.utils.BookStoreMessageTag;

/**
 * ReplicationRequest represents a replication request
 */
public class ReplicationRequest {
	private Set<?> dataSet;
	private BookStoreMessageTag messageType;

	public ReplicationRequest(Set<?> dataSet, BookStoreMessageTag messageType) {
		this.setDataSet(dataSet);
		this.setMessageType(messageType);
	}

	public Set<?> getDataSet() {
		return dataSet;
	}

	public void setDataSet(Set<?> dataSet) {
		this.dataSet = dataSet;
	}

	public BookStoreMessageTag getMessageType() {
		return messageType;
	}

	public void setMessageType(BookStoreMessageTag messageType) {
		this.messageType = messageType;
	}

}
