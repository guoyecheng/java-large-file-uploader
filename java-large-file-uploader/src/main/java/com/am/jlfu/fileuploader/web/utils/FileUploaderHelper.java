package com.am.jlfu.fileuploader.web.utils;


import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.IncorrectRequestException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.JsonObject;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.web.UploadServletParameter;
import com.google.gson.Gson;



/**
 * Provides some common methods to deal with file upload requests.
 * 
 * @author antoinem
 * 
 */
@Component
public class FileUploaderHelper {

	public class FileUploadConfiguration {

		private String fileId;
		private String crc;
		private InputStream inputStream;



		public FileUploadConfiguration() {
		}


		public String getFileId() {
			return fileId;
		}


		public void setFileId(String fileId) {
			this.fileId = fileId;
		}


		public String getCrc() {
			return crc;
		}


		public void setCrc(String crc) {
			this.crc = crc;
		}


		public InputStream getInputStream() {
			return inputStream;
		}


		public void setInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
		}


	}



	public FileUploadConfiguration extractFileUploadConfiguration(HttpServletRequest request)
			throws IncorrectRequestException, MissingParameterException, FileUploadException, IOException {
		final FileUploadConfiguration fileUploadConfiguration = new FileUploadConfiguration();

		// check if the request is multipart:
		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new IncorrectRequestException("Request should be multipart");
		}

		// extract the fields
		fileUploadConfiguration.setFileId(getParameterValue(request, UploadServletParameter.fileId));
		fileUploadConfiguration.setCrc(getParameterValue(request, UploadServletParameter.crc, false));

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
		fileUploadConfiguration.setInputStream(item.openStream());

		// return conf
		return fileUploadConfiguration;

	}


	public String getParameterValue(HttpServletRequest request, UploadServletParameter parameter)
			throws MissingParameterException {
		return getParameterValue(request, parameter, true);
	}


	public String getParameterValue(HttpServletRequest request, UploadServletParameter parameter, boolean mandatory)
			throws MissingParameterException {
		String parameterValue = request.getParameter(parameter.name());
		if (parameterValue == null && mandatory) {
			throw new MissingParameterException(parameter);
		}
		return parameterValue;
	}


	public void writeExceptionToResponse(final Exception e, ServletResponse servletResponse)
			throws IOException {
		writeToResponse(new SimpleJsonObject(e.getMessage()), servletResponse);
	}


	public void writeToResponse(JsonObject jsonObject, ServletResponse servletResponse)
			throws IOException {
		servletResponse.setContentType("application/json");
		servletResponse.getWriter().print(new Gson().toJson(jsonObject));
		servletResponse.getWriter().close();
	}

}
