package com.am.jlfu.fileuploader.web.utils;

import java.io.InputStream;

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