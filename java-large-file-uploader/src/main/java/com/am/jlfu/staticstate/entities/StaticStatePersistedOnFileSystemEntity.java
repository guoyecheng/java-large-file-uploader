package com.am.jlfu.staticstate.entities;


import java.io.Serializable;
import java.util.Map;

import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.staticstate.StaticStateManager;
import com.google.common.collect.Maps;



/**
 * Abstract class that is persisted on the filesystem and contains information about the files being
 * uploaded.<br>
 * You can of course extend it if you want to persist other stuff on the filesystem. If you do so,
 * you will have to call {@link StaticStateManager#init(Class)} with the type of the class you
 * defined extending this one.
 * 
 * @author antoinem
 * 
 */
public class StaticStatePersistedOnFileSystemEntity
		implements Serializable {

	/** generated id */
	private static final long serialVersionUID = 6033009138577295466L;

	/** The states of the files being uploaded, the string being its identifier. */
	private Map<String, StaticFileState> fileStates = Maps.newHashMap();

	/**
	 * The default rate of the client in kilo bytes.<br>
	 * Minimum should be set to 10kb/s.<br>
	 * Maximum is {@link Long#MAX_VALUE}<br>
	 * @see FileStateJsonBase#getRateInKiloBytes()
	 * @see FileStateJsonBase#setRateInKiloBytes(Long)
	 * */
	private Long defaultRateInKiloBytes;



	/**
	 * Default constructor.
	 */
	public StaticStatePersistedOnFileSystemEntity() {
		super();
	}


	public Map<String, StaticFileState> getFileStates() {
		return fileStates;
	}


	public void setFileStates(Map<String, StaticFileState> fileStates) {
		this.fileStates = fileStates;
	}


	public Long getDefaultRateInKiloBytes() {
		return defaultRateInKiloBytes;
	}


	public void setDefaultRateInKiloBytes(Long defaultRateInKiloBytes) {
		this.defaultRateInKiloBytes = defaultRateInKiloBytes;
	}


	
	

}
