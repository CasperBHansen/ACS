/**
 *
 */
package com.acertainbookstore.client.workloads;

import java.lang.Math;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 *
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 *
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		int numConcurrentWorkloadThreads = 10;
		String serverAddress = "http://localhost:8081";
		boolean localTest = true;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
        BookSetGenerator bookSetGenerator = null;
		if (localTest) {
            bookSetGenerator = new BookSetGenerator();
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
            bookSetGenerator = new BookSetGenerator();
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager, bookSetGenerator);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager, bookSetGenerator);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 *
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
		// TODO: You should aggregate metrics and output them for plotting here

		int successfulInteractions = 0;
		int totalRuns = 0;
		long elapsedTimeInNanoSecs = 0;
		int successfulFrequentBookStoreInteractionRuns = 0;
		int totalFrequentBookStoreInteractionRuns = 0;

		for (WorkerRunResult result : workerRunResults) {
			successfulInteractions += result.getSuccessfulInteractions();
			totalRuns += result.getTotalRuns();
			elapsedTimeInNanoSecs += result.getElapsedTimeInNanoSecs();
			successfulFrequentBookStoreInteractionRuns += result.getSuccessfulFrequentBookStoreInteractionRuns();
			totalFrequentBookStoreInteractionRuns += result.getTotalFrequentBookStoreInteractionRuns();
		}

		long aggregateTroughput = (successfulInteractions / elapsedTimeInNanoSecs);

		System.out.println("Successful Interactions: " + successfulInteractions);
		System.out.println("Successful Frequent Bookstore Interaction Runs: " + successfulFrequentBookStoreInteractionRuns);

		System.out.println("Total runs: " + totalRuns);
		System.out.println("Total Frequent Bookstore Interaction Runs: " + totalFrequentBookStoreInteractionRuns);

		System.out.println("Elapsed Time: " + elapsedTimeInNanoSecs + "ns");

		System.out.println("Aggregate Throughput: " + aggregateTroughput);
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 *
	 * Ignores the serverAddress if its a localTest
	 *
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager,
			BookSetGenerator bookSetGenerator) throws BookStoreException {

        Set<StockBook> booksToAdd = bookSetGenerator.getRandomSetOfStockBooks();

        // make sure we're working on a clean store
		stockManager.removeAllBooks();
		stockManager.addBooks(booksToAdd);

		// possible assertion test here, that stores match up.
	}
}
