package com.acertainbookstore.client.workloads;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {
	
	private Set<StockBook> books;

	public BookSetGenerator(Set<StockBook> bookSet) {
		books = bookSet;
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
		Set<Integer> offsets = new HashSet<Integer>();
		
		Integer offset = ThreadLocalRandom.current().nextInt(0, bound);
		for (int i = 0; i < num; ++num) {
			while (offsets.contains(offset)) {
				offset = ThreadLocalRandom.current().nextInt(0, bound);
			}
			offsets.add(offset);
		}
		
		return offsets;
	}

}
