package com.acertainbookstore.utils;

/**
 * 
 * Data Structure that we use to communicate objects and error messages from the
 * server to the client.
 * 
 */
public class BookStoreResponse {
	private BookStoreException exception = null;
	private BookStoreResult result = null;

	public BookStoreResponse() {

	}

	public BookStoreResponse(BookStoreException exception,
			BookStoreResult result) {
		this.setException(exception);
		this.setResult(result);
	}

	public BookStoreException getException() {
		return exception;
	}

	public void setException(BookStoreException exception) {
		this.exception = exception;
	}

	public BookStoreResult getResult() {
		return result;
	}

	public void setResult(BookStoreResult result) {
		this.result = result;
	}

}
