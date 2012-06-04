package com.am.jlfu.fileuploader.web;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.authorizer.Authorizer;
import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.PrepareUploadJson;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper;
import com.am.jlfu.fileuploader.web.utils.FileUploaderHelper;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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

	@Autowired
	Authorizer authorizer;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;



	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		log.trace("Handling request");

		Serializable jsonObject = null;
		try {
			// extract the action from the request
			UploadServletAction actionByParameterName =
					UploadServletAction.valueOf(fileUploaderHelper.getParameterValue(request, UploadServletParameter.action));

			// check authorization if its not get progress (because we do not really care about
			// authorization for get progress and it uses an array of file ids)
			if (!actionByParameterName.equals(UploadServletAction.getProgress)) {
				final String uuid = fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId, false);
				authorizer.getAuthorization(request, actionByParameterName, staticStateIdentifierManager.getIdentifier(),
						uuid != null ? UUID.fromString(uuid) : null);
			}

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
		log.debug("Processing action " + actionByParameterName.name());

		Serializable returnObject = null;
		switch (actionByParameterName) {
			case getConfig:
				returnObject = uploadProcessor.getConfig();
				break;
			case verifyCrcOfUncheckedPart:
				returnObject = verifyCrcOfUncheckedPart(request);
				break;
			case prepareUpload:
				returnObject = prepareUpload(request);
				break;
			case clearFile:
				uploadProcessor.clearFile(UUID.fromString(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId)));
				break;
			case clearAll:
				uploadProcessor.clearAll();
				break;
			case pauseFile:
				uploadProcessor.pauseFile(UUID.fromString(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId)));
				break;
			case resumeFile:
				returnObject =
						uploadProcessor.resumeFile(UUID.fromString(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId)));
				break;
			case setRate:
				uploadProcessor.setUploadRate(UUID.fromString(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId)),
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
		Collection<UUID> uuids = Collections2.transform(Arrays.asList(ids), new Function<String, UUID>() {

			@Override
			public UUID apply(String input) {
				return UUID.fromString(input);
			}

		});
		returnObject = Maps.newHashMap();
		for (UUID fileId : uuids) {
			try {
				Float progress = uploadProcessor.getProgress(fileId);
				Long uploadStat = uploadProcessor.getUploadStat(fileId);
				ProgressJson progressJson = new ProgressJson();
				progressJson.setProgress(progress);
				if (uploadStat != null) {
					progressJson.setUploadRate(uploadStat);
				}
				((HashMap<String, ProgressJson>) returnObject).put(fileId.toString(), progressJson);
			}
			catch (FileNotFoundException e) {
				log.debug("No progress will be retrieved for " + fileId + " because " + e.getMessage());
			}
		}
		return returnObject;
	}


	private Serializable prepareUpload(HttpServletRequest request)
			throws MissingParameterException, IOException {

		// extract file information
		PrepareUploadJson[] fromJson =
				new Gson()
						.fromJson(fileUploaderHelper.getParameterValue(request, UploadServletParameter.newFiles), PrepareUploadJson[].class);

		// prepare them
		final HashMap<String, UUID> prepareUpload = uploadProcessor.prepareUpload(fromJson);

		// return them
		return Maps.newHashMap(Maps.transformValues(prepareUpload, new Function<UUID, String>() {

			public String apply(UUID input) {
				return input.toString();
			};
		}));
	}


	private Boolean verifyCrcOfUncheckedPart(HttpServletRequest request)
			throws IOException, MissingParameterException {
		UUID fileId = UUID.fromString(fileUploaderHelper.getParameterValue(request, UploadServletParameter.fileId));
		try {
			uploadProcessor.verifyCrcOfUncheckedPart(fileId,
					fileUploaderHelper.getParameterValue(request, UploadServletParameter.crc));
		}
		catch (InvalidCrcException e) {
			// no need to log this exception, a fallback behaviour is defined in the
			// throwing method.
			// but we need to return something!
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}
}
