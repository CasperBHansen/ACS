package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyStockManager;
import com.acertainbookstore.interfaces.Replication;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedReadOnlyStockManager
 * interfaces
 * 
 * This class must also handle replication requests sent by the master
 * 
 */
public class SlaveCertainBookStore extends ReadOnlyCertainBookStore implements ReplicatedReadOnlyBookStore,
		ReplicatedReadOnlyStockManager, Replication {

	public SlaveCertainBookStore() {
		bookStore = new CertainBookStore();
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized void replicate(ReplicationRequest req) throws BookStoreException {

        switch (req.getMessageType()) {
            case REMOVEBOOKS:
                Set<Integer> bookSet = (Set<Integer>) req.getDataSet();
                bookStore.removeBooks(bookSet);
                break;

            case REMOVEALLBOOKS:
                bookStore.removeAllBooks();
                break;

            case ADDBOOKS:
                Set<StockBook> newBooks = (Set<StockBook>) req.getDataSet();
                bookStore.addBooks(newBooks);
                break;

            case ADDCOPIES:
                Set<BookCopy> listBookCopies = (Set<BookCopy>) req.getDataSet();
                bookStore.addCopies(listBookCopies);
                break;

            case UPDATEEDITORPICKS:
                Set<BookEditorPick> mapEditorPicksValues = (Set<BookEditorPick>) req.getDataSet();
                bookStore.updateEditorPicks(mapEditorPicksValues);
                break;

            case BUYBOOKS:
                Set<BookCopy> bookCopiesToBuy = (Set<BookCopy>) req.getDataSet();
                bookStore.buyBooks(bookCopiesToBuy);
                break;

            default:
                System.out.println("Unhandled replication message tag");
                break;

        }
	}

}
