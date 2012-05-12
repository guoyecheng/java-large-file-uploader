package com.am.jlfu.fileuploader.json;


import com.am.jlfu.fileuploader.logic.UploadProcessor;



public class FileStateJson
		extends FileStateJsonBase {

	private static final long serialVersionUID = 5043865795253104456L;

	/** Specifies whether the file is complete or not. */
	private boolean fileComplete;

	/** Bytes which have been completed. */
	private Long fileCompletionInBytes;

	/** the first chunk crc information */
	private CRCResult firstChunkCrc;
	

	/**
	 * Default constructor.
	 */
	public FileStateJson() {
		super();
	}


	public Boolean getFileComplete() {
		return fileComplete;
	}


	public void setFileComplete(Boolean fileComplete) {
		this.fileComplete = fileComplete;
	}


	public Long getFileCompletionInBytes() {
		return fileCompletionInBytes;
	}


	public void setFileCompletionInBytes(Long fileCompletionInBytes) {
		this.fileCompletionInBytes = fileCompletionInBytes;
	}
	



	public CRCResult getFirstChunkCrc() {
		return firstChunkCrc;
	}


	public void setFirstChunkCrc(CRCResult firstChunkCrc) {
		this.firstChunkCrc = firstChunkCrc;
	}


	public void setFileComplete(boolean fileComplete) {
		this.fileComplete = fileComplete;
	}


	

}
