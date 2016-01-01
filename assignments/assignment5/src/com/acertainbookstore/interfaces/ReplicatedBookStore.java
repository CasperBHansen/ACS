package com.acertainbookstore.interfaces;

import java.util.Set;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * ReplicatedBookStore declares a set of methods conforming to the BookStore
 * interface exposed by the bookstore to the proxies. These methods need to be
 * implemented by MasterCertainBookStore.
 */
public interface ReplicatedBookStore extends ReplicatedReadOnlyBookStore {

	/**
	 * Buy the sets of books specified.
	 * 
	 */
	public BookStoreResult buyBooks(Set<BookCopy> booksToBuy)
			throws BookStoreException;

	/**
	 * Applies the BookRatings in the set, i.e. rates each book with their
	 * respective rating.
	 * 
	 */
	public BookStoreResult rateBooks(Set<BookRating> bookRating)
			throws BookStoreException;

}
