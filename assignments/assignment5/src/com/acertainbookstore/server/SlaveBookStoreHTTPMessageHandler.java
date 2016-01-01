/**
 * 
 */
package com.acertainbookstore.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainbookstore.business.SlaveCertainBookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResponse;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * 
 * SlaveBookStoreHTTPMessageHandler implements the message handler class which
 * is invoked to handle messages received by the slave book store HTTP server It
 * decodes the HTTP message and invokes the SlaveCertainBookStore API
 * 
 */
public class SlaveBookStoreHTTPMessageHandler extends AbstractHandler {
	private SlaveCertainBookStore myBookStore = null;

	public SlaveBookStoreHTTPMessageHandler(SlaveCertainBookStore bookStore) {
		myBookStore = bookStore;
	}

	@SuppressWarnings("unchecked")
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		BookStoreMessageTag messageTag;
		String numBooksString = null;
		int numBooks = -1;
		String requestURI;
		BookStoreResponse bookStoreResponse = null;

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		requestURI = request.getRequestURI();

		// Need to do request multi-plexing
		if (!BookStoreUtility.isEmpty(requestURI)
				&& requestURI.toLowerCase().startsWith("/stock")) {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI
					.substring(6)); // the request is from store
			// manager, more
			// sophisticated security
			// features could be added
			// here
		} else {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI);
		}
		// the RequestURI before the switch
		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {

			// Write requests should not be handled
			switch (messageTag) {

			case LISTBOOKS:
				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.getBooks());
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			case GETBOOKS:
				String xml = BookStoreUtility
						.extractPOSTDataFromRequest(request);
				Set<Integer> isbnSet = (Set<Integer>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore.getBooks(isbnSet));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			case EDITORPICKS:
				numBooksString = URLDecoder
						.decode(request
								.getParameter(BookStoreConstants.BOOK_NUM_PARAM),
								"UTF-8");
				bookStoreResponse = new BookStoreResponse();
				try {
					numBooks = BookStoreUtility
							.convertStringToInt(numBooksString);
					bookStoreResponse.setResult(myBookStore
							.getEditorPicks(numBooks));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			case GETSTOCKBOOKSBYISBN:
				xml = BookStoreUtility.extractPOSTDataFromRequest(request);
				isbnSet = (Set<Integer>) BookStoreUtility
						.deserializeXMLStringToObject(xml);

				bookStoreResponse = new BookStoreResponse();
				try {
					bookStoreResponse.setResult(myBookStore
							.getBooksByISBN(isbnSet));
				} catch (BookStoreException ex) {
					bookStoreResponse.setException(ex);
				}
				response.getWriter().println(
						BookStoreUtility
								.serializeObjectToXMLString(bookStoreResponse));
				break;

			default:
				System.out.println("Unhandled message tag");
				break;
			}
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);

	}
}
