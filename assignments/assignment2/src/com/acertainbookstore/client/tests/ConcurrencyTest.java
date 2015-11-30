package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315;

/**
 * Test class to test the BookStore interface
 * 
 */
public class ConcurrencyTest {

	private static final int TEST_ISBN = 3044560;
	private static final int NUM_COPIES = 5;
	private static boolean localTest = true;
	private static StockManager storeManager;
	private static BookStore client;
	
	private class ClientBuyer implements Runnable {
		
		private BookStore client;
		private HashSet<BookCopy> copies;
		
		public ClientBuyer(BookStore client, HashSet<BookCopy> copies) {
			this.client = client;
			this.copies = copies;
		}
		
		public void run() {
			try {
				this.client.buyBooks(copies);
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}
	
	private class ClientAdder implements Runnable {
		
		private StockManager client;
		private HashSet<BookCopy> copies;

		public ClientAdder(StockManager client2, HashSet<BookCopy> copies) {
			this.client = client2;
			this.copies = copies;
		}
		
		public void run() {
			try {
				this.client.addCopies(copies);
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}
	
	private class ClientBuyThenReplenish implements Runnable {

		private BookStore client;
		private StockManager stock;
		private HashSet<BookCopy> copies;
		
		public ClientBuyThenReplenish(BookStore client, StockManager stock, HashSet<BookCopy> copies) {
			this.client = client;
			this.stock = stock;
			this.copies = copies;
		}
		
		public void run() {
			try {
				this.client.buyBooks(copies);
				this.stock.addCopies(copies);
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System
					.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean
					.parseBoolean(localTestProperty) : localTest;
			if (localTest) {
                ConcurrentCertainBookStore store = new ConcurrentCertainBookStore();
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

	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();
		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
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
		storeManager.addBooks(getStarWarsCollection());
	}

	/**
	 * Method to clean up the book store, execute after every test case is run
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
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

	/**
	 * Helper method to get the star wars trilogy
	 */
	private Set<StockBook> getStarWarsCollection() throws BookStoreException {
		Set<StockBook> starWarsCollection = new HashSet<StockBook>();
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 1, "A New Hope",
				"Alan Dean Foster & George Lucas", (float) 500, 5, 0, 0, 0, false));
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 2, "The Empire Strikes Back",
				"Donald F. Glut", (float) 500, 5, 0, 0, 0, false));
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 3, "Return of the Jedi",
				"James Kahn", (float) 500, 5, 0, 0, 0, false));
		
		return starWarsCollection;
	}

	/**
	 * Tests buying and adding books concurrently
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testOne() throws BookStoreException, InterruptedException {

		List<StockBook> booksBefore = storeManager.getBooks();
		
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 10)); // valid
		Thread C1 = new Thread(new ClientBuyer(client, booksToBuy));
		
		HashSet<BookCopy> booksToAdd = new HashSet<BookCopy>();
		booksToAdd.add(new BookCopy(TEST_ISBN, 5)); // valid
		Thread C2 = new Thread(new ClientAdder(storeManager, booksToAdd));

		C1.start();
		C2.start();
		
		C1.join();
		C2.join();
		
		List<StockBook> booksAfter = storeManager.getBooks();
		
		// horribly inefficient, but works
		for (StockBook before : booksBefore) {
			int a = before.getISBN();
			for (StockBook after : booksAfter) {
				int b = after.getISBN();
				if (a == b) {
					// temporarily, print it out
					System.out.println("ISBN: " + a);
					System.out.println(" · before:\t" + before.getNumCopies());
					System.out.println(" · after:\t" + after.getNumCopies() + "\n");
					assertTrue(before.getNumCopies() == after.getNumCopies());
				}
			}
		}
	}

	/**
	 * Tests buying and adding books concurrently
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testTwo() throws BookStoreException, InterruptedException {

		HashSet<BookCopy> starWarsCollection = new HashSet<BookCopy>();
		starWarsCollection.add(new BookCopy(TEST_ISBN + 1, 1));
		starWarsCollection.add(new BookCopy(TEST_ISBN + 2, 1));
		starWarsCollection.add(new BookCopy(TEST_ISBN + 3, 1));
		
		Thread C1 = new Thread(new ClientBuyThenReplenish(client, storeManager, starWarsCollection));
		// add C2
	}

}
