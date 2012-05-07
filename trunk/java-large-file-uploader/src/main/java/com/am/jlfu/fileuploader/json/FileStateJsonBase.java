package com.am.jlfu.fileuploader.json;


import com.am.jlfu.staticstate.entities.StaticStateRateConfiguration;



public class FileStateJsonBase
		implements JsonObject {

	private static final long serialVersionUID = 5043865795253104456L;

	/** The original file name. */
	private String originalFileName;

	/** The original file size */
	private Long originalFileSizeInBytes;

	/** rate conf */
	private StaticStateRateConfiguration staticStateRateConfiguration;



	/**
	 * Default constructor.
	 */
	public FileStateJsonBase() {
		super();
	}


	public String getOriginalFileName() {
		return originalFileName;
	}


	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}


	public Long getOriginalFileSizeInBytes() {
		return originalFileSizeInBytes;
	}


	public void setOriginalFileSizeInBytes(Long originalFileSizeInBytes) {
		this.originalFileSizeInBytes = originalFileSizeInBytes;
	}


	public StaticStateRateConfiguration getStaticStateRateConfiguration() {
		return staticStateRateConfiguration;
	}


	public void setStaticStateRateConfiguration(StaticStateRateConfiguration staticStateRateConfiguration) {
		this.staticStateRateConfiguration = staticStateRateConfiguration;
	}


}
