package com.acertainbookstore.utils;

import java.util.List;

/**
 * Data structure to represent a result from the bookstore
 */
public class BookStoreResult {
	private List<?> resultList;
	private long snapshotId;

	public BookStoreResult(List<?> resultList, long snapshotId) {
		this.setResultList(resultList);
		this.setSnapshotId(snapshotId);
	}

	public List<?> getResultList() {
		return resultList;
	}

	public void setResultList(List<?> resultList) {
		this.resultList = resultList;
	}

	public long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(long snapshotId) {
		this.snapshotId = snapshotId;
	}

}
