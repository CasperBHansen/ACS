package com.acertainbookstore.business.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

public class CertainBookStoreTest {

	private static final int TEST_ISBN = 3044560;
	private static final int NUM_COPIES = 5;
	private static boolean localTest = true;
	private static StockManager storeManager;
	private static BookStore client;

	@BeforeClass
	public static void setUpBeforeClass() {

		try {
			String localTestProperty = System
					.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean
					.parseBoolean(localTestProperty) : localTest;
			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy(
						"http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}
			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to add a book, executed before every test case is run
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}
	
	/**
	 * Method to clean up the book store, execute after every test case is run
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();
		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}

	/**
	 * Helper method to add some books
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones",
				"George RR Testin'", (float) 10, copies, 0, 0, 0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit",
				"JK Unit", (float) 10, NUM_COPIES, 0, 0, 0, false);
	}

	@Test
	public void testGetTopRatedBooks() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBooksInDemand() {
		fail("Not yet implemented");
	}

	@Test
	public void testRateBooksNotInStore() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1,
				"The Art of Computer Programming", "Donald Knuth", (float) 300,
				NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2,
				"The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES,
				0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);
		
		Set<BookRating> failRating = new HashSet<BookRating>();
		failRating.add(new BookRating(TEST_ISBN + 1, 3));
		failRating.add(new BookRating(TEST_ISBN + 2, 4));
		failRating.add(new BookRating(TEST_ISBN + 3, 5));
		
		try {
			client.rateBooks(failRating);
			fail();
		}
		catch (BookStoreException ex) {
			; // we expect this to fail
		}
		
		// no books should have been rated
		for (StockBook book : booksToAdd) {
			assertTrue(book.getTimesRated() == 0);
		}
	}

	@Test
	public void testRateBooksInvalidRating() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1,
				"The Art of Computer Programming", "Donald Knuth", (float) 300,
				NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2,
				"The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES,
				0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);
		
		Set<BookRating> failRating = new HashSet<BookRating>();
		failRating.add(new BookRating(TEST_ISBN + 1, 3));
		failRating.add(new BookRating(TEST_ISBN + 2, 15));
		
		try {
			client.rateBooks(failRating);
			fail();
		}
		catch (BookStoreException ex) {
			; // we expect this to fail
		}
		
		// no books should have been rated
		for (StockBook book : booksToAdd) {
			assertTrue(book.getTimesRated() == 0);
		}
	}

	@Test
	public void testRateBooksInvalidISBN() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1,
				"The Art of Computer Programming", "Donald Knuth", (float) 300,
				NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2,
				"The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES,
				0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);
		
		Set<BookRating> failISBN = new HashSet<BookRating>();
		failISBN.add(new BookRating(TEST_ISBN + 1, 3));
		failISBN.add(new BookRating(TEST_ISBN + 2, 3));
		failISBN.add(new BookRating(TEST_ISBN + 3, 3));
		
		try {
			client.rateBooks(failISBN);
			fail();
		}
		catch (BookStoreException ex) {
			; // we expect this to fail
		}
		
		// no books should have been rated
		for (StockBook book : booksToAdd) {
			assertTrue(book.getTimesRated() == 0);
		}
	}

}
