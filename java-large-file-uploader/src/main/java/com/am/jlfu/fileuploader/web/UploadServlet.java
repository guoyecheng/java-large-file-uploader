package com.am.jlfu.fileuploader.web;


import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.exception.BadRequestException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.JsonObject;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.google.gson.Gson;



/**
 * Uploads the file from the jquery uploader.
 * 
 * @author antoinem
 * 
 */
@Component("javaLargeFileUploaderServlet")
@WebServlet(name = "javaLargeFileUploaderServlet", urlPatterns = { "/javaLargeFileUploaderServlet" })
public class UploadServlet extends HttpRequestHandlerServlet
		implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadServlet.class);

	@Autowired
	UploadProcessor uploadProcessor;



	static String getParameterValue(HttpServletRequest request, UploadServletParameter parameter)
			throws MissingParameterException {
		String parameterValue = request.getParameter(parameter.name());
		if (parameterValue == null) {
			throw new MissingParameterException(parameter);
		}
		return parameterValue;
	}


	static void writeExceptionToResponse(final Exception e, ServletResponse servletResponse)
			throws IOException {
		writeToResponse(new SimpleJsonObject(e.getMessage()), servletResponse);
	}


	private static void writeToResponse(JsonObject jsonObject, ServletResponse servletResponse)
			throws IOException {
		servletResponse.setContentType("application/json");
		servletResponse.getWriter().print(new Gson().toJson(jsonObject));
		servletResponse.getWriter().close();
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

		}
		// If bad request, send a 400 error code
		catch (BadRequestException e) {
			log.error(e.getMessage(), e);
			writeExceptionToResponse(e, response);
		}
		// If unknown exception, send a 500 error code
		catch (Exception e) {
			log.error(e.getMessage(), e);
			writeExceptionToResponse(e, response);
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
			case clearFile:
				uploadProcessor.clearFile(getParameterValue(request, UploadServletParameter.fileId));
				break;
			case clearAll:
				uploadProcessor.clearAll();
				break;
			case setRate:
				uploadProcessor.setUploadRate(getParameterValue(request, UploadServletParameter.fileId),
						Long.valueOf(getParameterValue(request, UploadServletParameter.rate)));
				break;
			case getProgress:
				String fileId = getParameterValue(request, UploadServletParameter.fileId);
				Float progress = uploadProcessor.getProgress(fileId);
				Long uploadStat = uploadProcessor.getUploadStat(fileId);
				ProgressJson progressJson = new ProgressJson();
				progressJson.setProgress(progress);
				if (uploadStat != null) {
					progressJson.setUploadRate(uploadStat);
				}
				returnObject = progressJson;
				break;
		}
		return returnObject;
	}


}
