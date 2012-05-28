package com.am.jlfu.fileuploader.json;


import java.io.Serializable;



public class ProgressJson
		implements Serializable {

	/**
	 * generated id
	 */
	private static final long serialVersionUID = -8710522591230352636L;

	private Float progress;
	private Long uploadRate;



	public ProgressJson() {
	}


	public Float getProgress() {
		return progress;
	}


	public void setProgress(Float progress) {
		this.progress = progress;
	}


	public Long getUploadRate() {
		return uploadRate;
	}


	public void setUploadRate(Long uploadRate) {
		this.uploadRate = uploadRate;
	}


}
