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
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.ReplicationAwareBookStoreHTTPProxy;
import com.acertainbookstore.client.ReplicationAwareStockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Test class to test the failure of a slave
 *
 */
public class MasterFailTest {
	private static StockManager storeManager;
	private static BookStore client;

	private static final int TEST_ISBN = 3044560;
	private static final int NUM_COPIES = 5;
    private static final int TEST_STORE_SIZE = 100;
	private static boolean localTest = false;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
				storeManager = new ReplicationAwareStockManagerHTTPProxy();
				client = new ReplicationAwareBookStoreHTTPProxy();
			}

            // clean out the store to begin with
			storeManager.removeAllBooks();
            
            // add some books
            Set<StockBook> booksToAdd = new HashSet<StockBook>();
            for (int i = 0; i < TEST_STORE_SIZE; ++i) {
                StockBook book = new ImmutableStockBook(TEST_ISBN + i, "Test of Thrones",
                        "George RR Testin'", (float) 10, NUM_COPIES, 0, 0, 0, false);
                booksToAdd.add(book);
            }
            storeManager.addBooks(booksToAdd);
            
            System.out.println("Waiting for master server to crash..");
            Thread.sleep(5000);
            System.out.println("Assuming that the master server has crashed.");

		} catch (Exception e) {
			e.printStackTrace();
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

	/**
	 * Method to add a book, executed before every test case is run
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
        /* Not when we expect it to crash
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
        */
	}

	/**
	 * Method to clean up the book store, execute after every test case is run
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
        /* Not when we expect it to crash
		storeManager.removeAllBooks();
        */
	}

    /**
	 * Tests that writes aren't possible, but reads are.
	 */
	@Test
	public void testCrashedMaster() throws BookStoreException {
        
        // try to perform a write
        Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		try {
		    storeManager.addBooks(booksToAdd);
			fail();
		} catch (BookStoreException ex) {
            ;
		}
        
        // try getting all books (a read)
        Set<Integer> isbnSet = new HashSet<Integer>();
        for (Integer i = 0; i < TEST_STORE_SIZE; ++i) {
            isbnSet.add(TEST_ISBN + i);
        }

		try {
            List<Book> listBooks = client.getBooks(isbnSet);
		    assertTrue(listBooks.size() == TEST_STORE_SIZE);
        }
        catch (BookStoreException ex) {
            fail();
        }
	}

	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {

        try {
		    //storeManager.removeAllBooks();
        }
        catch (Exception ex) {
            ; // since the server crashed during test, we expect this to happen
        }
		if (!localTest) {
			((ReplicationAwareBookStoreHTTPProxy) client).stop();
			((ReplicationAwareStockManagerHTTPProxy) storeManager).stop();
		}
	}

}
