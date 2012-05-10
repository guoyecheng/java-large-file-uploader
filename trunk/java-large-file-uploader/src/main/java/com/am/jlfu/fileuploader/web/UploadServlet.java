package com.am.jlfu.fileuploader.web;


import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.json.JsonObject;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.am.jlfu.fileuploader.web.utils.FileUploaderHelper;



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

	@Autowired
	FileUploaderHelper fileUploaderHelper;



	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		log.trace("Handling request");

		JsonObject jsonObject = null;
		try {
			// extract the action from the request
			UploadServletAction actionByParameterName =
					UploadServletAction.valueOf(fileUploaderHelper.getParameterValue(request, UploadServletParameter.action));

			// then process the asked action
			jsonObject = processAction(actionByParameterName, request);


			// if something has to be written to the response
			if (jsonObject != null) {
				fileUploaderHelper.writeToResponse(jsonObject, response);
			}

		}
		// If exception, write it
		catch (Exception e) {
			log.error(e.getMessage(), e);
			fileUploaderHelper.writeExceptionToResponse(e, response);
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
			case verifyCrcOfUncheckedPart:
				uploadProcessor.verifyCrcOfUncheckedPart(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId),
						fileUploaderHelper.getParameterValue(request, UploadServletParameter.crc));
				break;
			case prepareUpload:
				String fileName = fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileName);
				Long size = Long.valueOf(fileUploaderHelper.getParameterValue(request, UploadServletParameter.size));
				String idOfTheFile = uploadProcessor.prepareUpload(size, fileName);
				returnObject = new SimpleJsonObject(idOfTheFile);
				break;
			case clearFile:
				uploadProcessor.clearFile(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
				break;
			case clearAll:
				uploadProcessor.clearAll();
				break;
			case pauseFile:
				uploadProcessor.pauseFile(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
				break;
			case resumeFile:
				uploadProcessor.resumeFile(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
				break;
			case setRate:
				uploadProcessor.setUploadRate(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId),
						Long.valueOf(fileUploaderHelper.getParameterValue(request, UploadServletParameter.rate)));
				break;
			case getProgress:
				String fileId = fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId);
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
