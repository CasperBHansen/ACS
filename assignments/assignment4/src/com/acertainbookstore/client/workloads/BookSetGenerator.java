package com.acertainbookstore.client.workloads;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.ImmutableStockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	private static int bookStoreSize = 100;

	private static SecureRandom random = new SecureRandom();
	private static Integer NEXT_ISBN = 1; // this is definitively the next isbn :P

    private Set<StockBook> books = new HashSet<StockBook>();

    /**
     * Constructs a new bookset generator
     */
    public BookSetGenerator() {
		for (int i = 0; i < bookStoreSize; ++i) {
			books.add(makeRandomBook());
		}
    }

	/**
	 * Returns num randomly selected isbns from the input set
	 *
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		Set<Integer> selected = new HashSet<Integer>();
		Set<Integer> offsets = generateRandomOffsets(num, 0, isbns.size() - 1);

		// select the isbns from the set, given the random offsets
		Integer offset = 0;
		for (Integer isbn : isbns) {
			if (offsets.contains(offset)) {
				selected.add(isbn);
			}
			++offset;
		}

		return selected;
	}

	public Set<StockBook> getRandomSetOfStockBooks() {
		int begin = (int) Math.floor(books.size()*0.25f);
		int end = (int) Math.floor(books.size()*0.75f);
		return nextSetOfStockBooks(randomInt(begin, end));
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 *
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> bookSet = new HashSet<StockBook>();
		Set<Integer> offsets = generateRandomOffsets(num, 0, books.size() - 1);

		Integer offset = 0;
		for (StockBook book : books) {
			if (offsets.contains(offset)) {
				bookSet.add(book);
			}
			++offset;
		}

		return bookSet;
	}

	/**
	 * Helper method which generates a random set of unique offsets
	 */
	private Set<Integer> generateRandomOffsets(int num, int min, int bound) {
        // please avoid going out of bounds
        bound = bound >= bookStoreSize ? bookStoreSize - 1 : bound;
        num = num > bound ? bound : num;

        /* bad way :P

		Set<Integer> offsets = new HashSet<Integer>();

		Integer offset = ThreadLocalRandom.current().nextInt(0, bound);
		for (int i = 0; i < num; ++i) {
			while (offsets.contains(offset)) {
				offset = ThreadLocalRandom.current().nextInt(0, bound);
			}
			offsets.add(offset);
		}

		return offsets;*/

        // make a set of available offsets
        ArrayList<Integer> available = new ArrayList<Integer>();
        for (int i = 0; i < bookStoreSize; ++i) { available.add(i); }

        Set<Integer> chosen = new HashSet<Integer>();
        Integer offset = 0;
        while(chosen.size() < num) {
            offset = available.remove(ThreadLocalRandom.current().nextInt(0, available.size() - 1));
            chosen.add(offset);
        }

        return chosen;
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
		int copies = randomInt(1, 10);
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
