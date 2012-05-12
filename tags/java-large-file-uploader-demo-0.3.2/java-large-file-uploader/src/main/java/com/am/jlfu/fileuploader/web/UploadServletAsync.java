package com.am.jlfu.fileuploader.web;


import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor.WriteChunkCompletionListener;
import com.am.jlfu.fileuploader.web.utils.FileUploadConfiguration;
import com.am.jlfu.fileuploader.web.utils.FileUploaderHelper;
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

	@Autowired
	FileUploaderHelper fileUploaderHelper;

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

			// extract stuff from request
			final FileUploadConfiguration process = fileUploaderHelper.extractFileUploadConfiguration(request);

			// process the request asynchronously
			final AsyncContext asyncContext = request.startAsync();
			asyncContext.setTimeout(taskTimeOut);


			// add a listener to clear bucket and close inputstream when process is complete or with
			// error
			asyncContext.addListener(new UploadServletAsyncListenerAdapter(process.getFileId()) {

				@Override
				void clean() {
					log.debug("closing input stream for " + process.getFileId());
					// close input stream
					IOUtils.closeQuietly(process.getInputStream());
					// and tell processor to clean its shit!
					uploadServletAsyncProcessor.clean(process.getFileId());
				}
			});

			// then process
			uploadServletAsyncProcessor.process(process.getFileId(), process.getCrc(), process.getInputStream(), new WriteChunkCompletionListener() {

				@Override
				public void success() {
					asyncContext.complete();
				}


				@Override
				public void error(Exception exception) {
					// treat the exception:

					// handles a stream ended unexpectedly , it just means the user has stopped the
					// stream
					if (exception.getMessage().equals("User has stopped streaming")) {
						log.warn("User has stopped streaming for file " + process.getFileId());
					}
					else if (exception.getMessage().equals("User cancellation")) {
						log.warn("User has cancelled streaming for file id " + process.getFileId());
						// do nothing
					}
					// if we have an unmanaged exception
					else {
						try {
							fileUploaderHelper.writeExceptionToResponse(exception, response);
						}
						catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
					asyncContext.complete();
				}

			});
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			fileUploaderHelper.writeExceptionToResponse(e, response);
		}

	}


}
