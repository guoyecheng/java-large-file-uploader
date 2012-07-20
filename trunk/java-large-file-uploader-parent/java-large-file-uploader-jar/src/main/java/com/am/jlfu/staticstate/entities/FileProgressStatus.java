package com.am.jlfu.staticstate.entities;

import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.utils.UnitConverter;

/**
 * Entity providing progress information about a file.
 * 
 * @author antoinem
 *
 */
public class FileProgressStatus extends ProgressJson{

	/**
	 * Generated id. 
	 */
	private static final long serialVersionUID = -6247365041854992033L;
	
	private long totalFileSize;
	private long bytesUploaded;

	/**
	 * Default constructor.
	 */
	public FileProgressStatus() {
		super();
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
		s+= "("+progress+"%)";
		if (uploadRate != null) {
			s+=  "at rate: "+UnitConverter.getFormattedSize(uploadRate) +"/s.";
		}
		if (estimatedRemainingTimeInSeconds != null) {
			s+= " Finishing in "+UnitConverter.getFormattedTime(estimatedRemainingTimeInSeconds)+".";
		}
		return s;
	}
	
	
}
