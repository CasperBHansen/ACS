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

import com.acertainbookstore.business.StockBook;
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
	private Random random = new Random();

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
		
		// Get stockManager and bookGenerator from configuration file.
		StockManager stm = configuration.getStockManager();
		BookSetGenerator bookGen = configuration.getBookSetGenerator();
		
		// Get all books from bookstore.
		List<StockBook> storeBooks = stm.getBooks();
		
		// Get random number of random set of books defined in the book generation class.
		int n = random.nextInt(10) + 1;
		Set<StockBook> randomBookSet = bookGen.nextSetOfStockBooks(n);
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
		
		// Get stockManager and bookGenerator from configuration file.
		StockManager stm = configuration.getStockManager();
		
		// Get all books from bookstore.
		List<StockBook> storeBooks = stm.getBooks();
		
		// Sort books on number of copies
		Collections.sort(storeBooks, new Comparator<StockBook>() {
			public int compare(StockBook a, StockBook b) {
				return a.getNumCopies() <= b.getNumCopies() ? 1 : -1;
			}
		});

		// Get random number of books to which we want the smallest number of copies.
		int k = random.nextInt(storeBooks.size() - 1) + 1;

		// Add the wanted subset of books.
		stm.addBooks( new HashSet<StockBook>(storeBooks.subList(0, k)) );
	}

	/**
	 * Runs the customer interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
		
	}

}
