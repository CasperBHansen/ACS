/**
 * 
 */
package com.acertainbookstore.server;

import com.acertainbookstore.business.MasterCertainBookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Starts the master bookstore HTTP server.
 */
public class MasterBookStoreHTTPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws BookStoreException {
		MasterCertainBookStore bookStore = new MasterCertainBookStore();
		int listen_on_port = 8081;
		MasterBookStoreHTTPMessageHandler handler = new MasterBookStoreHTTPMessageHandler(
				bookStore);
		String server_port_string = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_SERVER_PORT);
		if (server_port_string != null) {
			try {
				listen_on_port = Integer.parseInt(server_port_string);
			} catch (NumberFormatException ex) {
				System.err.println(ex);
			}
		}
		if (BookStoreHTTPServerUtility.createServer(listen_on_port, handler)) {
			;
		}
	}

}
