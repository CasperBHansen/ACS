/**
 * 
 */
package com.acertainbookstore.interfaces;

import com.acertainbookstore.business.ReplicationRequest;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Replication defines the methods that can be invoked by the master bookstore
 * on the slave bookstores
 */
public interface Replication {

	void replicate(ReplicationRequest req) throws BookStoreException;
}
