package com.acertainbookstore.business;

import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.BookStoreException;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedReadOnlyStockManager
 * interfaces
 * 
 * This class must also handle replication requests sent by the master
 * 
 */
public class SlaveCertainBookStore extends ReadOnlyCertainBookStore implements ReplicatedReadOnlyBookStore,
		ReplicatedReadOnlyStockManager, Replication {

	public SlaveCertainBookStore() {
		bookStore = new CertainBookStore();
	}

	@Override
	public synchronized void replicate(ReplicationRequest req) throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

}
