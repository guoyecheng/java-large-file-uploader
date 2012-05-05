package com.am.jlfu.fileuploader.web;


import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.exception.BadRequestException;
import com.am.jlfu.fileuploader.exception.IncorrectRequestException;
import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.JsonObject;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.google.gson.Gson;



/**
 * Uploads the file from the jquery uploader.
 * 
 * @author antoinem
 * 
 */
@Component("javaFileUploaderManagerServlet")
@WebServlet(name = "javaFileUploaderManagerServlet", urlPatterns = { "/javaFileUploaderManagerServlet" })
public class UploadServlet extends HttpRequestHandlerServlet
		implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadServlet.class);

	@Autowired
	UploadProcessor uploadProcessor;



	private String getParameterValue(HttpServletRequest request, UploadServletParameter parameter)
			throws MissingParameterException {
		String parameterValue = request.getParameter(parameter.name());
		if (parameterValue == null) {
			throw new MissingParameterException(parameter);
		}
		return parameterValue;
	}


	private void writeExceptionToResponse(final Exception e, HttpServletResponse response)
			throws IOException {
		writeToResponse(new SimpleJsonObject(e.getMessage()), response);
	}


	private void writeToResponse(JsonObject jsonObject, HttpServletResponse response)
			throws IOException {
		response.setContentType("application/json");
		response.getWriter().print(new Gson().toJson(jsonObject));
		response.getWriter().close();
	}


	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		log.trace("Handling request");

		JsonObject jsonObject = null;
		try {
			// extract the action from the request
			UploadServletAction actionByParameterName =
					UploadServletAction.valueOf(getParameterValue(request, UploadServletParameter.action));

			// then process the asked action
			jsonObject = processAction(actionByParameterName, request);


			// if something has to be written to the response
			if (jsonObject != null) {
				writeToResponse(jsonObject, response);
			}

			// set success
			response.setStatus(200);
		}
		// handles a stream ended unexpectedly , it just means the user has stopped the stream
		catch (IOException e) {
			if (e.getCause() instanceof MalformedStreamException) {
				log.warn("User has stopped streaming.");
			}
			else {
				log.error(e.getMessage(), e);
				writeExceptionToResponse(e, response);
				response.sendError(500);
			}
		}
		// If bad request, send a 400 error code
		catch (BadRequestException e) {
			log.error(e.getMessage(), e);
			writeExceptionToResponse(e, response);
			response.sendError(400);
		}
		// If unknown exception, send a 500 error code
		catch (Exception e) {
			log.error(e.getMessage(), e);
			writeExceptionToResponse(e, response);
			response.sendError(500);
		}

	}


	private JsonObject processAction(UploadServletAction actionByParameterName, HttpServletRequest request)
			throws Exception {
		log.trace("Processing action " + actionByParameterName.name());

		JsonObject returnObject = null;
		switch (actionByParameterName) {
			case getConfig:
				returnObject = uploadProcessor.getConfig();
				break;
			case prepareUpload:
				String fileName = getParameterValue(request, UploadServletParameter.fileName);
				Long size = Long.valueOf(getParameterValue(request, UploadServletParameter.size));
				String idOfTheFile = uploadProcessor.prepareUpload(size, fileName);
				returnObject = new SimpleJsonObject(idOfTheFile);
				break;
			case upload:
				processUpload(request);
				break;
			case clearFile:
				uploadProcessor.clearFile(getParameterValue(request, UploadServletParameter.fileId));
				break;
			case clearAll:
				uploadProcessor.clearAll();
				break;
			case getProgress:
				Float progress = uploadProcessor.getProgress(getParameterValue(request, UploadServletParameter.fileId));
				returnObject = new SimpleJsonObject(progress.toString());
				break;
		}
		return returnObject;
	}


	public void processUpload(HttpServletRequest req)
			throws IOException, IncorrectRequestException, MissingParameterException, FileUploadException, InvalidCrcException {

		// check if the request is multipart:
		if (!ServletFileUpload.isMultipartContent(req)) {
			throw new IncorrectRequestException("Request should be multipart");
		}

		// extract the fields
		String fileId = getParameterValue(req, UploadServletParameter.fileId);
		Long sliceFrom = Long.valueOf(getParameterValue(req, UploadServletParameter.sliceFrom));
		String crc = getParameterValue(req, UploadServletParameter.crc);
		

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		// parse the request
		FileItemIterator iter = upload.getItemIterator(req);
		FileItemStream item = iter.next();

		// throw exception if item is null
		if (item == null) {
			throw new IncorrectRequestException("No file to upload found in the request");
		}

		// TODO catch exception from process?
		// process
		uploadProcessor.process(item.openStream(), sliceFrom, fileId, crc);

	}


}
