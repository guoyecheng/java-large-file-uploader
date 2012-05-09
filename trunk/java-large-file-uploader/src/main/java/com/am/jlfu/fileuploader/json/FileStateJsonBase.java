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
	 * */
	private Long rateInKiloBytes;

	/**
	 * Amount of bytes that were correctly validated.<br>
	 * When resuming an upload, all bytes in the file that have not been validated are revalidated.
	 */
	private long crcedBytes;



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


	public Long getCrcedBytes() {
		return crcedBytes;
	}


	public void setCrcedBytes(Long crcedBytes) {
		this.crcedBytes = crcedBytes;
	}


}
