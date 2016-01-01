package com.acertainbookstore.interfaces;

import java.util.Set;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * ReplicatedReadOnlyStockManager declares a set of read only methods conforming
 * to StockManager interface exposed by the bookstore to the proxies. These
 * methods need to be implemented by SlaveCertainBookStore.
 */
public interface ReplicatedReadOnlyStockManager {

	/**
	 * Returns the list of books in the bookstore
	 * 
	 */
	public BookStoreResult getBooks() throws BookStoreException;

	/**
	 * Returns the books matching the set of ISBNs given, is different to
	 * getBooks in the BookStore interface because of the return type of the
	 * books
	 * 
	 */
	public BookStoreResult getBooksByISBN(Set<Integer> isbns)
			throws BookStoreException;

	/**
	 * Returns the list of books which has sale miss
	 * 
	 */
	public BookStoreResult getBooksInDemand() throws BookStoreException;

}
