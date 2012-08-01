package com.am.jlfu.fileuploader.limiter;


public class RequestUploadProcessingConfiguration extends UploadProcessingConfiguration {

	/**
	 * Boolean specifying whether the upload is processing or not.
	 */
	private volatile boolean isProcessing;

	/** 
	 * Boolean specifying whether the client uploading the file is telling the server that the client will close the stream and that the server shall expect it.
	 */
	private volatile boolean expectingStreamClose;

	
	public boolean isProcessing() {
		return isProcessing;
	}


	public void setProcessing(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}


	public void expectStreamClose() {
		this.expectingStreamClose = true;
	}

	public void resetExpectStreamClose() {
		this.expectingStreamClose = false;
	}
	
	public boolean isStreamExpectedToBeClosed() {
		return this.expectingStreamClose;
	}


}
