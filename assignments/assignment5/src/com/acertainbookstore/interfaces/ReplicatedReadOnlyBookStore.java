package com.acertainbookstore.interfaces;

import java.util.Set;

import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * ReplicatedReadOnlyBookStore declares a set of read only methods conforming to
 * the BookStore interface exposed by the bookstore to the proxies. These
 * methods need to be implemented by SlaveCertainBookStore.
 * 
 */
public interface ReplicatedReadOnlyBookStore {

	/**
	 * Returns the list of books corresponding to the set of ISBNs
	 * 
	 */
	public BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException;

	/**
	 * Return a list of top rated numBooks books.
	 * 
	 */
	public BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException;

	/**
	 * Returns the list of books containing numBooks editor picks
	 * 
	 */
	public BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException;

}
