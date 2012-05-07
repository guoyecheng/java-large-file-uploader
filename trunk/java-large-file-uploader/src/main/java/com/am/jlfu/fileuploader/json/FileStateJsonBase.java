package com.am.jlfu.fileuploader.json;





public class FileStateJsonBase
		implements JsonObject {

	private static final long serialVersionUID = 5043865795253104456L;

	/** The original file name. */
	private String originalFileName;

	/** The original file size */
	private Long originalFileSizeInBytes;


	/**
	 * The rate of the client in kilo bytes.<br>
	 * Minimum should be set to 10kb/s.<br>
	 * Maximum is {@link Long#MAX_VALUE}<br>
	 * */
	private Long rateInKiloBytes;



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


	public Long getRateInKiloBytes() {
		return rateInKiloBytes;
	}


	public void setRateInKiloBytes(Long rateInKiloBytes) {
		this.rateInKiloBytes = rateInKiloBytes;
	}

}
