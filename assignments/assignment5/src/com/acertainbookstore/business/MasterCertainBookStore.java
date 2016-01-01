package com.acertainbookstore.business;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.acertainbookstore.interfaces.ReplicatedBookStore;
import com.acertainbookstore.interfaces.ReplicatedStockManager;
import com.acertainbookstore.interfaces.Replicator;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * MasterCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedBookStore and ReplicatedStockManager interfaces
 * 
 * This class also contains a Replicator which replicates updates to slaves.
 * 
 * 
 */
public class MasterCertainBookStore extends ReadOnlyCertainBookStore
		implements ReplicatedBookStore, ReplicatedStockManager {
	private Replicator replicator = null;
	private String filePath = "/universe/acertainbookstore/server.properties";

	public MasterCertainBookStore() throws BookStoreException {
		Set<String> slaveServers = initializeSlaveMapping();
		bookStore = new CertainBookStore();
		// the threadpool size is equal to number of slaves for which concurrent
		// requests need to be sent
		replicator = new CertainBookStoreReplicator(slaveServers.size(), slaveServers);
	}

	private Set<String> initializeSlaveMapping() throws BookStoreException {
		Properties props = new Properties();
		Set<String> slaveServers = new HashSet<String>();

		try {
			props.load(new FileInputStream(filePath));
		} catch (IOException ex) {
			throw new BookStoreException(ex);
		}

		String slaveAddresses = props.getProperty(BookStoreConstants.KEY_SLAVE);
		for (String slave : slaveAddresses.split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
			if (!slave.toLowerCase().startsWith("http://")) {
				slave = new String("http://" + slave);
			}
			if (!slave.endsWith("/")) {
				slave = new String(slave + "/");
			}
			slaveServers.add(slave);
		}
		return slaveServers;

	}

	private void waitForSlaveUpdates(List<Future<ReplicationResult>> replicatedSlaveFutures) {
		Set<String> faultySlaveServers = new HashSet<String>();
		for (Future<ReplicationResult> slaveServer : replicatedSlaveFutures) {
			while (true) {
				// We want a non-cancellable get() so do it in a loop until
				// successful to ignore interrupted exceptions
				try {
					// block until the future result is available
					ReplicationResult result = slaveServer.get();
					if (!result.isReplicationSuccessful()) {
						faultySlaveServers.add(result.getServerAddress());
					}
					break;
				} catch (InterruptedException e) {
					// Current thread was interrupted, there is no terminate
					// semantics so just ignore it and retry
				} catch (ExecutionException e) {
					// Some exception happened in the replicator thread - this
					// should not happen,
					// crash the process -> fail stop
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}

		if (faultySlaveServers.size() > 0) {
			replicator.markServersFaulty(faultySlaveServers);
		}
	}

	public synchronized BookStoreResult addBooks(Set<StockBook> bookSet) throws BookStoreException {

		ReplicationRequest request = new ReplicationRequest(bookSet, BookStoreMessageTag.ADDBOOKS);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.addBooks(bookSet); // If this fails it will throw an exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

	public synchronized BookStoreResult addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		ReplicationRequest request = new ReplicationRequest(bookCopiesSet, BookStoreMessageTag.ADDCOPIES);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.addCopies(bookCopiesSet); // If this fails it will throw an
											// exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

	public synchronized BookStoreResult updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		ReplicationRequest request = new ReplicationRequest(editorPicks, BookStoreMessageTag.UPDATEEDITORPICKS);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.updateEditorPicks(editorPicks); // If this fails it will throw
													// an exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

	public synchronized BookStoreResult buyBooks(Set<BookCopy> booksToBuy) throws BookStoreException {
		ReplicationRequest request = new ReplicationRequest(booksToBuy, BookStoreMessageTag.BUYBOOKS);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.buyBooks(booksToBuy); // If this fails it will throw an
										// exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

	public synchronized BookStoreResult rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		throw new BookStoreException("Not implemented");
	}

	public synchronized BookStoreResult removeAllBooks() throws BookStoreException {
		ReplicationRequest request = new ReplicationRequest(null, BookStoreMessageTag.REMOVEALLBOOKS);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.removeAllBooks(); // If this fails it will throw an
									// exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

	public synchronized BookStoreResult removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		ReplicationRequest request = new ReplicationRequest(isbnSet, BookStoreMessageTag.REMOVEBOOKS);
		List<Future<ReplicationResult>> replicatedSlaveFutures = replicator.replicate(request);
		bookStore.removeBooks(isbnSet); // If this fails it will throw an
										// exception
		snapshotId++;
		waitForSlaveUpdates(replicatedSlaveFutures);
		BookStoreResult result = new BookStoreResult(null, snapshotId);
		return result;
	}

}
