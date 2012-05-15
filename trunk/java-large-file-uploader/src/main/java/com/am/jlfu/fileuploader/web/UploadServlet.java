package com.am.jlfu.fileuploader.web;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.PrepareUploadJson;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper;
import com.am.jlfu.fileuploader.web.utils.FileUploaderHelper;
import com.google.common.collect.Maps;
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

	@Autowired
	FileUploaderHelper fileUploaderHelper;

	@Autowired
	ExceptionCodeMappingHelper exceptionCodeMappingHelper;



	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		log.trace("Handling request");

		Serializable jsonObject = null;
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
			exceptionCodeMappingHelper.processException(e, response);
		}

	}


	private Serializable processAction(UploadServletAction actionByParameterName, HttpServletRequest request)
			throws Exception {
		log.trace("Processing action " + actionByParameterName.name());

		Serializable returnObject = null;
		switch (actionByParameterName) {
			case getConfig:
				returnObject = uploadProcessor.getConfig();
				break;
			case verifyCrcOfUncheckedPart:
				verifyCrcOfUncheckedPart(request);
				break;
			case getCrcOfFirstChunk:
				returnObject = uploadProcessor.getCRCOfFirstChunk(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
				break;
			case prepareUpload:
				returnObject = prepareUpload(request);
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
				returnObject = uploadProcessor.resumeFile(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
				break;
			case setRate:
				uploadProcessor.setUploadRate(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId),
						Long.valueOf(fileUploaderHelper.getParameterValue(request, UploadServletParameter.rate)));
				break;
			case getProgress:
				returnObject = getProgress(request);
				break;
		}
		return returnObject;
	}


	private Serializable getProgress(HttpServletRequest request)
			throws MissingParameterException {
		Serializable returnObject;
		String[] ids =
				new Gson()
						.fromJson(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId), String[].class);
		returnObject = Maps.newHashMap();
		for (String fileId : ids) {
			try {
				Float progress = uploadProcessor.getProgress(fileId);
				Long uploadStat = uploadProcessor.getUploadStat(fileId);
				ProgressJson progressJson = new ProgressJson();
				progressJson.setProgress(progress);
				if (uploadStat != null) {
					progressJson.setUploadRate(uploadStat);
				}
				((HashMap<String, ProgressJson>) returnObject).put(fileId, progressJson);
			}
			catch (FileNotFoundException e) {
				log.debug("No progress will be retrieved for " + fileId + " because " + e.getMessage());
			}
		}
		return returnObject;
	}


	private Serializable prepareUpload(HttpServletRequest request)
			throws MissingParameterException, IOException {
		Serializable returnObject;
		PrepareUploadJson[] fromJson =
				new Gson()
						.fromJson(fileUploaderHelper.getParameterValue(request, UploadServletParameter.newFiles), PrepareUploadJson[].class);
		returnObject = Maps.newHashMap();
		for (PrepareUploadJson prepareUploadJson : fromJson) {
			String idOfTheFile = uploadProcessor.prepareUpload(prepareUploadJson.getSize(), prepareUploadJson.getFileName());
			((HashMap<String, String>) returnObject).put(prepareUploadJson.getTempId().toString(), idOfTheFile);
		}
		return returnObject;
	}


	private void verifyCrcOfUncheckedPart(HttpServletRequest request)
			throws IOException, MissingParameterException {
		try {
			uploadProcessor.verifyCrcOfUncheckedPart(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId),
					fileUploaderHelper.getParameterValue(request, UploadServletParameter.crc));
		}
		catch (InvalidCrcException e) {
			// no need to log this exception, a fallback behaviour is defined in the
			// throwing method.
		}
	}
}
