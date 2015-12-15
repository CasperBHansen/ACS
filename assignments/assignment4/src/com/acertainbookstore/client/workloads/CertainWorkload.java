/**
 * 
 */
package com.acertainbookstore.client.workloads;

import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.ImmutableStockBook;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

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
	
	private static int bookStoreSize = 1000;
	
	private static SecureRandom random = new SecureRandom();
	private static Integer NEXT_ISBN = 1; // this is definitively the next isbn :P

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
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
			
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}
		
		Set<StockBook> randomizedBooks = null;
		
		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager, randomizedBooks);
		
		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);
		
		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager, randomizedBooks);
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
			Set<StockBook> randomizedBooks) throws BookStoreException {
		
		stockManager.removeAllBooks(); // make sure we're working on a clean store
		
		randomizedBooks = new HashSet<StockBook>();
		for (int i = 0; i < bookStoreSize; ++i) {
			randomizedBooks.add(makeRandomBook());
		}
		
		stockManager.addBooks(randomizedBooks);
		
		// possible assertion test here, that stores match up.
	}
	

	
	private static Integer getNextISBN() {
		return NEXT_ISBN++;
	}
	
	private static String randomString() {
		// happily stolen from stackoverflow.com
		return new BigInteger(130, random).toString(32); // todo: slice
	}
	
	private static float randomFloat(float min, float max) {
		return (float)ThreadLocalRandom.current().nextDouble(min, max);
	}
	
	private static int randomInt(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max);
	}
	
	private static ImmutableStockBook makeRandomBook() {
		String title = randomString();
		String author = randomString();
		float price = randomFloat(5, 100);
		int copies = randomInt(0, 10);
		long misses = (long)randomInt(0, 10);
		long rated = (long)randomInt(0, 10);
		long rating = (long)0;
		
		// total rating only makes sense if it has any
		if (rated > 0) {
			rating = randomInt(0, 5);
		}
		
		boolean picked = false;
		if (randomInt(0, 9) == 0) {
			picked = true;
		}
		
		return new ImmutableStockBook(getNextISBN(), title, author, price, copies, misses, rated, rating, picked);
	}
}
