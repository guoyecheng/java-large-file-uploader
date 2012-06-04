package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;



@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=rateLimiterConfiguration")
public class RateLimiterConfigurationManager {

	private static final Logger log = LoggerFactory.getLogger(RateLimiterConfigurationManager.class);

	/** Client is evicted from the map when not accessed for that duration */
	public static final int CLIENT_EVICTION_TIME_IN_MINUTES = 2;

	@Autowired
	private JLFUListenerPropagator jlfuListenerPropagator;

	final LoadingCache<UUID, UploadProcessingConfiguration> clientConfigMap = CacheBuilder.newBuilder()
			.removalListener(new RemovalListener<UUID, UploadProcessingConfiguration>() {

				@Override
				public void onRemoval(RemovalNotification<UUID, UploadProcessingConfiguration> notification) {
					jlfuListenerPropagator.getPropagator().onClientInactivity(notification.getKey(), CLIENT_EVICTION_TIME_IN_MINUTES);
				}
			})
			.expireAfterAccess(CLIENT_EVICTION_TIME_IN_MINUTES, TimeUnit.MINUTES)
			.build(new CacheLoader<UUID, UploadProcessingConfiguration>() {

				@Override
				public UploadProcessingConfiguration load(UUID arg0)
						throws Exception {
					log.trace("Created new bucket for client with id #{}", arg0);
					return new UploadProcessingConfiguration();
				}
			});

	final LoadingCache<UUID, RequestUploadProcessingConfiguration> requestConfigMap = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new CacheLoader<UUID, RequestUploadProcessingConfiguration>() {

				@Override
				public RequestUploadProcessingConfiguration load(UUID arg0)
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
	@Value("${jlfu.ratelimiter.maximumRatePerClientInKiloBytes:10240}")
	private volatile long maximumRatePerClientInKiloBytes;


	// 10mb/s
	@Value("${jlfu.ratelimiter.maximumOverAllRateInKiloBytes:10240}")
	private volatile long maximumOverAllRateInKiloBytes;



	// ///////////////


	/**
	 * Specify that a request has to be cancelled, the file is scheduled for deletion.
	 * 
	 * @param fileId
	 * @return true if there was a pending upload for this file.
	 */
	public boolean markRequestHasShallBeCancelled(UUID fileId) {
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


	public boolean requestIsReset(UUID fileId) {
		RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest && !unchecked.isProcessing();
	}


	public boolean requestHasToBeCancelled(UUID fileId) {
		RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		return unchecked.cancelRequest;
	}


	public Set<Entry<UUID, UploadProcessingConfiguration>> getClientEntries() {
		return clientConfigMap.asMap().entrySet();
	}


	public Set<Entry<UUID, RequestUploadProcessingConfiguration>> getRequestEntries() {
		return requestConfigMap.asMap().entrySet();
	}


	public void reset(UUID fileId) {
		final RequestUploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		unchecked.cancelRequest = false;
		unchecked.setProcessing(false);
	}


	public long getAllowance(UUID fileId) {
		return requestConfigMap.getUnchecked(fileId).getDownloadAllowanceForIteration();
	}


	public void assignRateToRequest(UUID fileId, Long rateInKiloBytes) {
		requestConfigMap.getUnchecked(fileId).rateInKiloBytes = rateInKiloBytes;
	}


	public Long getUploadState(UUID requestIdentifier) {
		return requestConfigMap.getUnchecked(requestIdentifier).getInstantRateInBytes();
	}


	public RequestUploadProcessingConfiguration getRequestUploadProcessingConfiguration(UUID fileId) {
		return requestConfigMap.getUnchecked(fileId);
	}


	public UploadProcessingConfiguration getClientUploadProcessingConfiguration(UUID clientId) {
		return clientConfigMap.getUnchecked(clientId);
	}


	public void pause(UUID fileId) {
		requestConfigMap.getUnchecked(fileId).setPaused(true);
	}


	public void resume(UUID fileId) {
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
