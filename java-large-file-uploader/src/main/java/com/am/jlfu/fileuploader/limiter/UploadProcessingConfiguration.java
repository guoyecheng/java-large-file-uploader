package com.am.jlfu.fileuploader.limiter;

public class UploadProcessingConfiguration {

	/**
	 * Specifies the amount of bytes that have been written
	 * */
	private long bytesWritten;
	private Object bytesWrittenLock = new Object();

	/**
	 * Specifies the amount of bytes that can be uploaded for an iteration of the refill process
	 * of {@link RateLimiter}
	 * */
	private long downloadAllowanceForIteration;
	private Object downloadAllowanceForIterationLock = new Object();

	/**
	 * Boolean specifying whether that request shall be cancelled (and the relating streams
	 * closed)<br>
	 * 
	 * @see RateLimiterConfigurationManager#markRequestHasShallBeCancelled(String)
	 * @see UploadProcessingConfigurationManager#requestHasToBeCancelled(String))
	 * */
	volatile boolean cancelRequest;

	/**
	 * The desired upload rate for this request. <br>
	 * Can be null (the default rate is applied).
	 */
	volatile Long rateInKiloBytes;

	/**
	 * Boolean specifying whether the upload is paused or not.
	 */
	volatile boolean isProcessing = false;


	/**
	 * The statistics.
	 * 
	 * @return
	 */
	long instantRateInBytes;



	public Long getRateInKiloBytes() {
		return rateInKiloBytes;
	}


	public boolean isProcessing() {
		return isProcessing;
	}


	public long getDownloadAllowanceForIteration() {
		synchronized (downloadAllowanceForIterationLock) {
			return downloadAllowanceForIteration;
		}
	}


	void setDownloadAllowanceForIteration(long downloadAllowanceForIteration) {
		synchronized (downloadAllowanceForIterationLock) {
			this.downloadAllowanceForIteration = downloadAllowanceForIteration;
		}
	}

	public long getAndResetBytesWritten() {
		synchronized (bytesWrittenLock) {
			final long temp = bytesWritten;
			bytesWritten = 0;
			return temp;
		}
	}

	/**
	 * Specifies the bytes that have been read from the files.
	 * 
	 * @param bytesConsumed
	 */
	public void bytesConsumedFromAllowance(long bytesConsumed) {
		synchronized (bytesWrittenLock) {
			synchronized (downloadAllowanceForIterationLock) {
				bytesWritten += bytesConsumed;
				downloadAllowanceForIteration -= bytesConsumed;
			}
		}
	}


	void setInstantRateInBytes(long instantRateInBytes) {
		this.instantRateInBytes = instantRateInBytes;
	}


	long getInstantRateInBytes() {
		return instantRateInBytes;
	}

	public void setProcessing(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}
}