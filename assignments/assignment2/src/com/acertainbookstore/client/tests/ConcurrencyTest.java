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

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
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
			boolean succesful = false;
			while (!succesful) {
				try {
					this.client.buyBooks(copies);
					succesful = true;
				}
				catch (BookStoreException ex) {
					//System.out.println(ex + ", for the sake of testing, we try again ^^");
				}
			}
		}
	}
	
	private class ClientAdder implements Runnable {
		
		private StockManager client;
		private HashSet<BookCopy> copies;

		public ClientAdder(StockManager client, HashSet<BookCopy> copies) {
			this.client = client;
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
	
	private class ClientBuyCopies implements Runnable {
		
		private ResultWrapper result;
		private BookStore client;
		private HashSet<BookCopy> copies;
		
		public ClientBuyCopies(BookStore client, HashSet<BookCopy> copies, ResultWrapper result) {
			this.client = client;
			this.copies = copies;
			this.result = result;
		}
		
		public void run() {
			try {
				this.client.buyBooks(copies);
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
				result.setResult(true);
			}
		}
	}
	
	private class ClientContinuousGetBooks implements Runnable {
		private StockManager stock;
		private HashSet<BookCopy> copies;
		private int bought;
		private long time;
		
		public ClientContinuousGetBooks(StockManager stock, HashSet<BookCopy> copies, int bought, long time) {
			this.stock = stock;
			this.copies = copies;
			this.bought = bought;
			this.time = time;
		}
		
		public void run() {
			try {
				long startTime = System.currentTimeMillis();
				
				while ((System.currentTimeMillis() - startTime) < time) {
					List<StockBook> books = this.stock.getBooks();
					for (StockBook book : books) {
						assertTrue(book.getNumCopies() == NUM_COPIES ||
								   book.getNumCopies() == (NUM_COPIES - bought));
					}
				}
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}
	
	private class ClientUpdateEditorPicks implements Runnable {
		private StockManager stock;
		private HashSet<BookEditorPick> editorPicks;
		
		public ClientUpdateEditorPicks(StockManager stock, HashSet<BookEditorPick> editorPicks) {
			this.stock = stock;
			this.editorPicks = editorPicks;
		}
		
		public void run() {
			try {
				stock.updateEditorPicks(editorPicks);
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}
	
	private class ClientGetEditorPicks implements Runnable {
		private BookStore store;
		private HashSet<BookEditorPick> editorPicks;
		
		public ClientGetEditorPicks(BookStore store, HashSet<BookEditorPick> editorPicks) {
			this.store = store;
			this.editorPicks = editorPicks;
		}
		
		public void run() {
			try {
				List<Book> picks = store.getEditorPicks(editorPicks.size());
				for (BookEditorPick pick : editorPicks) {
					boolean found = false;
					int isbn = pick.getISBN();
					
					for (Book book : picks) {
						if (book.getISBN() == isbn) {
							found = true;
						}
					}
					
					assertTrue(found);
				}
			}
			catch (BookStoreException ex) {
				System.out.println(ex);
			}
		}
	}
	
	private class ClientReads implements Runnable {
		private StockManager stock;
		private int numTimes;
		
		public ClientReads(StockManager stock, int numTimes) {
			this.stock = stock;
			this.numTimes = numTimes;
		}
		
		public void run() {
			try {
				for (int i = 0; i < numTimes; ++i) {
					stock.getBooks();
				}
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
		storeManager.addBooks(getStarWarsCollection(NUM_COPIES));
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
	private Set<StockBook> getStarWarsCollection(int copies) throws BookStoreException {
		Set<StockBook> starWarsCollection = new HashSet<StockBook>();
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 1, "A New Hope",
				"Alan Dean Foster & George Lucas", (float) 500, copies, 0, 0, 0, false));
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 2, "The Empire Strikes Back",
				"Donald F. Glut", (float) 500, copies, 0, 0, 0, false));
		starWarsCollection.add(new ImmutableStockBook(TEST_ISBN + 3, "Return of the Jedi",
				"James Kahn", (float) 500, copies, 0, 0, 0, false));
		
		return starWarsCollection;
	}

	/**
	 * Tests buying and adding books concurrently
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testOne() throws BookStoreException, InterruptedException {
		
		int copies = 50000;
		List<StockBook> booksBefore = storeManager.getBooks();
		
		HashSet<BookCopy> bookCopies = new HashSet<BookCopy>();
		bookCopies.add(new BookCopy(TEST_ISBN, copies));
		
		Thread C1 = new Thread(new ClientBuyer(client, bookCopies));
		Thread C2 = new Thread(new ClientAdder(storeManager, bookCopies));

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
		
		long continousTime = 2000;
		int copiesBought = 1;
		
		HashSet<BookCopy> starWarsCollection = new HashSet<BookCopy>();
		starWarsCollection.add(new BookCopy(TEST_ISBN + 1, copiesBought));
		starWarsCollection.add(new BookCopy(TEST_ISBN + 2, copiesBought));
		starWarsCollection.add(new BookCopy(TEST_ISBN + 3, copiesBought));
		
		Thread C1 = new Thread(new ClientBuyThenReplenish(client, storeManager, starWarsCollection));
		Thread C2 = new Thread(new ClientContinuousGetBooks(storeManager, starWarsCollection, copiesBought, continousTime));
		
		C1.start();
		C2.start();
		
		C1.join();
		C2.join();
	}

	/**
	 * Tests concurrent reads, measured by time.
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testTime() throws BookStoreException, InterruptedException {
		
		int divide = 4;
		int numTimes = 800 * divide;
		
		// serial baseline
		Thread serialThread = new Thread(new ClientReads(storeManager, numTimes));
		long serialBefore = System.currentTimeMillis();
		serialThread.start();
		serialThread.join();
		long serialAfter = System.currentTimeMillis();
		long serialTime = serialAfter - serialBefore;
		
		System.out.println("Serial took " + serialTime);
		
		Thread concurrentA = new Thread(new ClientReads(storeManager, numTimes / divide));
		Thread concurrentB = new Thread(new ClientReads(storeManager, numTimes / divide));
		Thread concurrentC = new Thread(new ClientReads(storeManager, numTimes / divide));
		Thread concurrentD = new Thread(new ClientReads(storeManager, numTimes / divide));
		
		long concurrentBefore = System.currentTimeMillis();
		concurrentA.start();
		concurrentB.start();
		concurrentC.start();
		concurrentD.start();
		
		concurrentA.join();
		concurrentB.join();
		concurrentC.join();
		concurrentD.join();
		long concurrentAfter = System.currentTimeMillis();
		long concurrentTime = concurrentAfter - concurrentBefore;
		
		System.out.println("Concurrently took " + concurrentTime);
		
		assertTrue(concurrentTime <= serialTime);
	}

	/**
	 * Tests buying and adding books concurrently
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testBuyTwice() throws BookStoreException, InterruptedException {
		
		ResultWrapper result = new ResultWrapper(); // ingenious solution, I know ^^
		
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		Thread C1 = new Thread(new ClientBuyCopies(client, booksToBuy, result));
		Thread C2 = new Thread(new ClientBuyCopies(client, booksToBuy, result));
		
		C1.start();
		C2.start();
	
		C1.join();
		C2.join();
		
		// the intention is that result is true if it met an exception
		assertTrue(result.getResult());
	}
	
	private class ResultWrapper {
		private boolean result = false;
		
		public void setResult(boolean value) {
			this.result = value;
		}
		
		public boolean getResult() {
			return this.result;
		}
	}

	/**
	 * Tests updating and reading editor picks concurrently
	 * 
	 * @throws InterruptedException, InterruptedException
	 */
	@Test
	public void testEditorPicks() throws BookStoreException, InterruptedException {
		
		HashSet<BookEditorPick> editorPicks = new HashSet<BookEditorPick>();
		editorPicks.add(new BookEditorPick(TEST_ISBN + 1, true));
		editorPicks.add(new BookEditorPick(TEST_ISBN + 2, true));
		editorPicks.add(new BookEditorPick(TEST_ISBN + 3, true));

		Thread C1 = new Thread(new ClientUpdateEditorPicks(storeManager, editorPicks));
		Thread C2 = new Thread(new ClientGetEditorPicks(client, editorPicks));
		
		C1.start();
		C2.start();
	
		C1.join();
		C2.join();
	}
}
