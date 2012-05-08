package com.am.jlfu.fileuploader.web;


import java.io.IOException;
import java.io.InputStream;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.exception.IncorrectRequestException;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor.WriteChunkCompletionListener;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;



@Component("javaLargeFileUploaderAsyncServlet")
@WebServlet(name = "javaLargeFileUploaderAsyncServlet", urlPatterns = { "/javaLargeFileUploaderAsyncServlet" }, asyncSupported = true)
public class UploadServletAsync extends HttpRequestHandlerServlet
		implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsync.class);

	@Autowired
	UploadServletAsyncProcessor uploadServletAsyncProcessor;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	/**
	 * Maximum time that a streaming request can take.<br>
	 * Note that the pause/resume stuff might not be a good idea as it keeps the stream opened while
	 * paused.
	 */
	// TODO make configurable
	private long taskTimeOut = DateUtils.MILLIS_PER_HOUR;



	@Override
	public void handleRequest(HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		// process the request
		try {

			// check if the request is multipart:
			if (!ServletFileUpload.isMultipartContent(request)) {
				throw new IncorrectRequestException("Request should be multipart");
			}

			// extract the fields
			final String fileId = UploadServlet.getParameterValue(request, UploadServletParameter.fileId);
			String crc = UploadServlet.getParameterValue(request, UploadServletParameter.crc);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload();

			// parse the requestuest
			FileItemIterator iter = upload.getItemIterator(request);
			FileItemStream item = iter.next();

			// throw exception if item is null
			if (item == null) {
				throw new IncorrectRequestException("No file to upload found in the request");
			}

			// extract input stream
			final InputStream inputStream = item.openStream();


			// process the request asynchronously
			final AsyncContext asyncContext = request.startAsync();
			asyncContext.setTimeout(taskTimeOut);


			// add a listener to clear bucket and close inputstream when process is complete or with
			// error
			asyncContext.addListener(new UploadServletAsyncListenerAdapter(fileId) {

				@Override
				void clean() {
					log.debug("closing input stream for " + fileId);
					// close input stream
					IOUtils.closeQuietly(inputStream);
					// and tell processor to clean its shit!
					uploadServletAsyncProcessor.clean(fileId);
				}
			});

			// then process
			uploadServletAsyncProcessor.process(fileId, crc, inputStream, new WriteChunkCompletionListener() {

				@Override
				public void success() {
					asyncContext.complete();
				}


				@Override
				public void error(Exception exception) {
					try {
						UploadServlet.writeExceptionToResponse(exception, response);
					}
					catch (IOException e) {
						log.error(e.getMessage(), e);
					}
					asyncContext.complete();
				}

			});
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			UploadServlet.writeExceptionToResponse(e, response);
		}

	}


}
