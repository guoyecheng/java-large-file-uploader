package com.am.jlfu.fileuploader.json;


import java.util.Map;



/**
 * The configuration.
 * 
 * @author antoinem
 * 
 */
public class InitializationConfiguration
		implements JsonObject {

	private static final long serialVersionUID = -6955613223772661218L;

	/**
	 * The size of the slice in bytes.
	 */
	private Float inByte;

	/**
	 * The list of the pending files.
	 */
	private Map<String, FileStateJson> pendingFiles;



	/**
	 * Default constructor
	 */
	public InitializationConfiguration() {
		super();
	}


	public Float getInByte() {
		return inByte;
	}


	public void setInByte(Float inByte) {
		this.inByte = inByte;
	}


	public Map<String, FileStateJson> getPendingFiles() {
		return pendingFiles;
	}


	public void setPendingFiles(Map<String, FileStateJson> pendingFiles) {
		this.pendingFiles = pendingFiles;
	}



}
