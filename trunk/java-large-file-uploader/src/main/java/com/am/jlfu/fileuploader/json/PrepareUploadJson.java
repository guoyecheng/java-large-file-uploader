package com.am.jlfu.fileuploader.json;


import java.io.Serializable;



public class PrepareUploadJson
		implements Serializable {

	private Integer tempId;

	private String fileName;

	private Long size;



	/**
	 * Default constructor.
	 */
	public PrepareUploadJson() {
		super();
	}


	public Integer getTempId() {
		return tempId;
	}


	public void setTempId(Integer tempId) {
		this.tempId = tempId;
	}


	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public Long getSize() {
		return size;
	}


	public void setSize(Long size) {
		this.size = size;
	}


}
