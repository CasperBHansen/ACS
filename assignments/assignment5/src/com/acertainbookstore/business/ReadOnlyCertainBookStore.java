package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

public class ReadOnlyCertainBookStore implements ReplicatedReadOnlyBookStore, ReplicatedReadOnlyStockManager{
	protected CertainBookStore bookStore = null;
	protected long snapshotId = 0;

	public ReadOnlyCertainBookStore() {
		bookStore = new CertainBookStore();
	}
	
	public synchronized BookStoreResult getBooks() throws BookStoreException {
		BookStoreResult result = new BookStoreResult(bookStore.getBooks(),
				snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooks(ISBNList), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getEditorPicks(numBooks), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksByISBN(Set<Integer> isbns)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooksByISBN(isbns), snapshotId);
		return result;
	}

}
