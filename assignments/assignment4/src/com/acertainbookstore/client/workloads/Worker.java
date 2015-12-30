/**
 *
 */
package com.acertainbookstore.client.workloads;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 *
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 *
 */
public class Worker implements Callable<WorkerRunResult> {
	private WorkloadConfiguration configuration = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;

	public Worker(WorkloadConfiguration config) {
		configuration = config;
	}

	/**
	 * Run the appropriate interaction while trying to maintain the configured
	 * distributions
	 *
	 * Updates the counts of total runs and successful runs for customer
	 * interaction
	 *
	 * @param chooseInteraction
	 * @return
	 */
	private boolean runInteraction(float chooseInteraction) {
		try {
			if (chooseInteraction < configuration
					.getPercentRareStockManagerInteraction()) {
				runRareStockManagerInteraction();
			} else if (chooseInteraction < configuration
					.getPercentFrequentStockManagerInteraction()) {
				runFrequentStockManagerInteraction();
			} else {
				numTotalFrequentBookStoreInteraction++;
				runFrequentBookStoreInteraction();
				numSuccessfulFrequentBookStoreInteraction++;
			}
		} catch (BookStoreException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Run the workloads trying to respect the distributions of the interactions
	 * and return result in the end
	 */
	public WorkerRunResult call() throws Exception {
		int count = 1;
		long startTimeInNanoSecs = 0;
		long endTimeInNanoSecs = 0;
		int successfulInteractions = 0;
		long timeForRunsInNanoSecs = 0;

		Random rand = new Random();
		float chooseInteraction;

		// Perform the warmup runs
		while (count++ <= configuration.getWarmUpRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			runInteraction(chooseInteraction);
		}

		count = 1;
		numTotalFrequentBookStoreInteraction = 0;
		numSuccessfulFrequentBookStoreInteraction = 0;

		// Perform the actual runs
		startTimeInNanoSecs = System.nanoTime();
		while (count++ <= configuration.getNumActualRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			if (runInteraction(chooseInteraction)) {
				successfulInteractions++;
			}
		}
		endTimeInNanoSecs = System.nanoTime();
		timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
		return new WorkerRunResult(successfulInteractions,
				timeForRunsInNanoSecs, configuration.getNumActualRuns(),
				numSuccessfulFrequentBookStoreInteraction,
				numTotalFrequentBookStoreInteraction);
	}

	/**
	 * Runs the new stock acquisition interaction
	 *
	 * @throws BookStoreException
	 */
	private void runRareStockManagerInteraction() throws BookStoreException {

		// Get StockManager and BookSetGenerator from configuration file.
		StockManager stm = configuration.getStockManager();
		BookSetGenerator bookGen = configuration.getBookSetGenerator();

		if (stm == null) {
			throw new BookStoreException("StockManager is null");
		}

		if (bookGen == null) {
			throw new BookStoreException("BookSetGenerator is null");
		}

		// Get all books from bookstore.
		List<StockBook> storeBooks = stm.getBooks();

		if (storeBooks == null) {
			throw new BookStoreException("storeBooks is null");
		}

		Set<StockBook> randomBookSet = bookGen.nextSetOfStockBooks(configuration.getNumBooksToAdd());
		Set<Integer> isbnOfRandom = new HashSet<Integer>();

		for (StockBook book : randomBookSet) {
			isbnOfRandom.add(book.getISBN());
		}

		Set<StockBook> booksNotFound = new HashSet<StockBook>();

		// Check if books are in the bookstore.
		for (StockBook book : storeBooks) {
			if (!isbnOfRandom.contains(book.getISBN())) {
				booksNotFound.add(book);
			}
		}
		stm.addBooks(booksNotFound);
	}

	/**
	 * Runs the stock replenishment interaction
	 *
	 * @throws BookStoreException
	 */
	private void runFrequentStockManagerInteraction() throws BookStoreException {

		// Get StockManager from configuration file.
		StockManager stm = configuration.getStockManager();

		if (stm == null) {
			throw new BookStoreException("StockManager is null");
		}

		// Get all books from bookstore.
		List<StockBook> storeBooks = stm.getBooks();

		if (storeBooks == null) {
			throw new BookStoreException("storeBooks is null");
		}

		// Sort books on number of copies
		Collections.sort(storeBooks, new Comparator<StockBook>() {
			public int compare(StockBook a, StockBook b) {

                if (a.getNumCopies() < b.getNumCopies()) {
                    return 1;
                }

                if (a.getNumCopies() > b.getNumCopies()) {
                    return -1;
                }

                return 0;

                // return (a.getNumCopies() <= b.getNumCopies()) ? 1 : -1;
			}
		});

		// Add the wanted subset of books.
		stm.addBooks( new HashSet<StockBook>(storeBooks.subList(0, configuration.getNumBooksWithLeastCopies())) );
	}

	/**
	 * Runs the customer interaction
	 *
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {

		// Get BookStore from configuration file.
		BookStore bkst = configuration.getBookStore();

		if (bkst == null) {
			throw new BookStoreException("BookStore is null");
		}

		// Get random number of books to which we want the smallest number of copies.

		List<Book> editorPicks = bkst.getEditorPicks(configuration.getNumEditorPicksToGet())
													 .subList(0, configuration.getNumBooksToBuy());

		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();

		for (Book book : editorPicks) {
			booksToBuy.add( new BookCopy(book.getISBN(), configuration.getNumBookCopiesToBuy()) );
		}

		bkst.buyBooks( booksToBuy );
	}

}
