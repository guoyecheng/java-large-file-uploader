package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;



@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=rateLimiterConfiguration")
public class RateLimiterConfigurationManager {

	private static final Logger log = LoggerFactory.getLogger(RateLimiterConfigurationManager.class);


	final LoadingCache<String, UploadProcessingConfiguration> clientConfigMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, UploadProcessingConfiguration>() {

				@Override
				public UploadProcessingConfiguration load(String arg0)
						throws Exception {
					log.trace("Created new bucket for client with id #{}", arg0);
					return new UploadProcessingConfiguration();
				}
			});

	final LoadingCache<String, RequestUploadProcessingConfiguration> requestConfigMap = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, RequestUploadProcessingConfiguration>() {

				@Override
				public RequestUploadProcessingConfiguration load(String arg0)
						throws Exception {
					log.trace("Created new bucket for request with id #{}", arg0);
					return new RequestUploadProcessingConfiguration();
				}
			});

	private UploadProcessingConfiguration masterProcessingConfiguration = new UploadProcessingConfiguration();

	// ///////////////
	// Configuration//
	// ///////////////

	// 10mb/s
	private volatile long maximumRatePerClientInKiloBytes = 10 * 1024;

	// 10mb/s
	private volatile long maximumOverAllRateInKiloBytes = 10 * 1024;



	// ///////////////


	/**
	 * Specify that a request has to be cancelled, the file is scheduled for deletion.
	 * 
	 * @param fileId
	 * @return true if there was a pending upload for this file.
	 */
	public boolean markRequestHasShallBeCancelled(String fileId) {
		RequestUploadProcessingConfiguration ifPresent = requestConfigMap.getIfPresent(fileId);
		// if we have a value in the map
		if (ifPresent != null) {
			RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
			// and if this file is currently being processed
			if (unchecked.isProcessing()) {
				// we ask for cancellation
				unchecked.cancelRequest = true;
			}
			// we return true if the file was processing, false otherwise
			return unchecked.isProcessing();
		}
		// if we dont have a value in the map, there is no pending upload
		else {
			// we can return false
			return false;
		}
	}


	public boolean requestIsReset(String fileId) {
		RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest && !unchecked.isProcessing();
	}


	public boolean requestHasToBeCancelled(String fileId) {
		RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest;
	}


	public Set<Entry<String, UploadProcessingConfiguration>> getClientEntries() {
		return clientConfigMap.asMap().entrySet();
	}


	public Set<Entry<String, RequestUploadProcessingConfiguration>> getRequestEntries() {
		return requestConfigMap.asMap().entrySet();
	}


	public void reset(String fileId) {
		final RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		unchecked.cancelRequest = false;
		unchecked.setProcessing(false);
	}


	public long getAllowance(String fileId) {
		return requestConfigMap.getUnchecked(fileId).getDownloadAllowanceForIteration();
	}


	public void assignRateToRequest(String fileId, Long rateInKiloBytes) {
		requestConfigMap.getUnchecked(fileId).rateInKiloBytes = rateInKiloBytes;
	}


	public Long getUploadState(String requestIdentifier) {
		return requestConfigMap.getUnchecked(requestIdentifier).getInstantRateInBytes();
	}


	public RequestUploadProcessingConfiguration getRequestUploadProcessingConfiguration(String fileId) {
		return requestConfigMap.getUnchecked(fileId);
	}


	public UploadProcessingConfiguration getClientUploadProcessingConfiguration(String clientId) {
		return clientConfigMap.getUnchecked(clientId);
	}


	public void pause(String fileId) {
		requestConfigMap.getUnchecked(fileId).setPaused(true);
	}


	public void resume(String fileId) {
		requestConfigMap.getUnchecked(fileId).setPaused(false);
	}


	@ManagedAttribute
	public long getMaximumRatePerClientInKiloBytes() {
		return maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumRatePerClientInKiloBytes(long maximumRatePerClientInKiloBytes) {
		this.maximumRatePerClientInKiloBytes = maximumRatePerClientInKiloBytes;
	}


	@ManagedAttribute
	public long getMaximumOverAllRateInKiloBytes() {
		return maximumOverAllRateInKiloBytes;
	}


	@ManagedAttribute
	public void setMaximumOverAllRateInKiloBytes(long maximumOverAllRateInKiloBytes) {
		this.maximumOverAllRateInKiloBytes = maximumOverAllRateInKiloBytes;
	}


	public UploadProcessingConfiguration getMasterProcessingConfiguration() {
		return masterProcessingConfiguration;
	}
}
