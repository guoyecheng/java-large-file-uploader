package com.am.jlfu.staticstate.entities;

import java.io.Serializable;

/**
 * Entity providing progress information about a file.
 * 
 * @author antoinem
 *
 */
public class FileProgressStatus implements Serializable{

	/**
	 * Generated id. 
	 */
	private static final long serialVersionUID = -6247365041854992033L;
	
	private float percentageCompleted;
	private long totalFileSize;
	private long bytesUploaded;
	
	/**
	 * Default constructor.
	 */
	public FileProgressStatus() {
	}

	
	public float getPercentageCompleted() {
		return percentageCompleted;
	}

	
	public void setPercentageCompleted(float percentageCompleted) {
		this.percentageCompleted = percentageCompleted;
	}

	
	public long getTotalFileSize() {
		return totalFileSize;
	}

	
	public void setTotalFileSize(long totalFileSize) {
		this.totalFileSize = totalFileSize;
	}

	
	public long getBytesUploaded() {
		return bytesUploaded;
	}

	
	public void setBytesUploaded(long bytesUploaded) {
		this.bytesUploaded = bytesUploaded;
	}
	
	@Override
	public String toString() {
		String s = "";
		s+= "Uploaded "+bytesUploaded;
		s+= "/"+totalFileSize+" Bytes";
		s+= "("+percentageCompleted+"%)";
		return s;
	}
	
}
