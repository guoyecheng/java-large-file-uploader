package com.am.jlfu.fileuploader.web;


import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor.WriteChunkCompletionListener;
import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper;
import com.am.jlfu.fileuploader.web.utils.FileUploadConfiguration;
import com.am.jlfu.fileuploader.web.utils.FileUploaderHelper;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@Component("javaLargeFileUploaderAsyncServlet")
@WebServlet(name = "javaLargeFileUploaderAsyncServlet", urlPatterns = { "/javaLargeFileUploaderAsyncServlet" }, asyncSupported = true)
public class UploadServletAsync extends HttpRequestHandlerServlet
		implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsync.class);

	@Autowired
	ExceptionCodeMappingHelper exceptionCodeMappingHelper;

	@Autowired
	UploadServletAsyncProcessor uploadServletAsyncProcessor;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	FileUploaderHelper fileUploaderHelper;

	/**
	 * Maximum time that a streaming request can take.<br>
	 * Note that the pause/resume stuff might not be a good idea as it keeps the stream opened while
	 * paused.
	 */
	private long taskTimeOut = DateUtils.MILLIS_PER_HOUR;



	@Override
	public void handleRequest(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		// process the request
		try {

			// extract stuff from request
			final FileUploadConfiguration process = fileUploaderHelper.extractFileUploadConfiguration(request);

			// check if the file is paused or not, if paused, we do not process this request
			if (uploadServletAsyncProcessor.isPausedOrCancelled(process.getFileId())) {
				log.debug(process.getFileId() + " is paused, request ignored.");
				response.sendError(499);
				return;
			}

			// get the model
			final StaticStatePersistedOnFileSystemEntity entityIfPresent = staticStateManager.getEntityIfPresent();
			if (entityIfPresent == null) {
				log.debug(process.getFileId() + " is cancelled, request ignored.");
				// if not present, its cancelled, but the client can handle this as paused.
				response.sendError(499);
				return;
			}
			StaticFileState fileState = entityIfPresent.getFileStates().get(process.getFileId());
			if (fileState == null) {
				throw new FileNotFoundException("File with id " + process.getFileId() + " not found");
			}

			// process the request asynchronously
			final AsyncContext asyncContext = request.startAsync();
			asyncContext.setTimeout(taskTimeOut);


			// add a listener to clear bucket and close inputstream when process is complete or
			// with
			// error
			asyncContext.addListener(new UploadServletAsyncListenerAdapter(process.getFileId()) {

				@Override
				void clean() {
					log.debug("request " + request + " completed.");
					// we do not need to clear the inputstream here.
					// and tell processor to clean its shit!
					uploadServletAsyncProcessor.clean(process.getFileId());
				}
			});

			// then process
			uploadServletAsyncProcessor.process(fileState, process.getFileId(), process.getCrc(), process.getInputStream(),
					new WriteChunkCompletionListener() {

						@Override
						public void success() {
							asyncContext.complete();
						}


						@Override
						public void error(Exception exception) {
							// handles a stream ended unexpectedly , it just means the user has
							// stopped the
							// stream
							if (exception.getMessage() != null) {
								if (exception.getMessage().equals("Stream ended unexpectedly")) {
									log.warn("User has stopped streaming for file " + process.getFileId());
								}
								else if (exception.getMessage().equals("User cancellation")) {
									log.warn("User has cancelled streaming for file id " + process.getFileId());
									// do nothing
								}
								else {
									exceptionCodeMappingHelper.processException(exception, response);
								}
							}
							else {
								exceptionCodeMappingHelper.processException(exception, response);
							}

							asyncContext.complete();
						}

					});
		}
		catch (Exception e) {
			exceptionCodeMappingHelper.processException(e, response);
		}

	}

}
