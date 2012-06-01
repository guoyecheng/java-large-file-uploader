package com.am.jlfu.fileuploader.limiter;


public class RequestUploadProcessingConfiguration extends UploadProcessingConfiguration {

	/**
	 * Boolean specifying whether that request shall be cancelled (and the relating streams
	 * closed)<br>
	 * 
	 * @see RateLimiterConfigurationManager#markRequestHasShallBeCancelled(String)
	 * @see UploadProcessingConfigurationManager#requestHasToBeCancelled(String))
	 * */
	volatile boolean cancelRequest;

	/**
	 * Boolean specifying whether the upload is processing or not.
	 */
	private volatile boolean isProcessing = false;

	/**
	 * Boolean specifying whether the upload is paused or not.
	 */
	private volatile boolean isPaused = false;



	public boolean isProcessing() {
		return isProcessing;
	}


	public void setProcessing(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}


	public boolean isPaused() {
		return isPaused;
	}


	public void setPaused(boolean isPaused) {
		this.isPaused = isPaused;
	}


}
