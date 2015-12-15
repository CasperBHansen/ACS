package com.acertainbookstore.client.workloads;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import com.acertainbookstore.business.ImmutableBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {
	
	private Set<StockBook> books;
	
	private int size;
	
	private static SecureRandom random = new SecureRandom();
	private static Integer NEXT_ISBN = 1; // this is definitively the next isbn :P

	public BookSetGenerator(int size) {
		this.size = size;
		
		books = new HashSet<StockBook>();
		
		for (int i = 0; i < size; ++i) {
			books.add(makeRandomBook());
		}
	}
	
	private int getSize() {
		return size;
	}
	
	private Integer getNextISBN() {
		return NEXT_ISBN++;
	}
	
	private String randomString() {
		// happily stolen from stackoverflow.com
		return new BigInteger(130, random).toString(32); // todo: slice
	}
	
	private float randomFloat(float min, float max) {
		return (float)ThreadLocalRandom.current().nextDouble(min, max);
	}
	
	private int randomInt(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max);
	}
	
	private ImmutableStockBook makeRandomBook() {
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
