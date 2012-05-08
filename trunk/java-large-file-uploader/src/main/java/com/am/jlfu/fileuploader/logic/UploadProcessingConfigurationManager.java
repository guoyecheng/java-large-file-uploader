package com.am.jlfu.fileuploader.logic;


import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.limiter.RateLimiter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;



@Component
public class UploadProcessingConfigurationManager {

	private static final Logger log = LoggerFactory.getLogger(UploadProcessingConfigurationManager.class);

	final LoadingCache<String, UploadProcessingConfiguration> requestConfigMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, UploadProcessingConfiguration>() {

				@Override
				public UploadProcessingConfiguration load(String arg0)
						throws Exception {
					log.trace("Created new bucket for #{}", arg0);
					return new UploadProcessingConfiguration();
				}
			});



	public class UploadProcessingConfiguration {

		/**
		 * Semaphore used by {@link RateLimiter}
		 * */
		private Semaphore semaphore = new Semaphore(0, false);

		/**
		 * Boolean specifying whether that request shall be cancelled (and the relating streams
		 * closed)<br>
		 * 
		 * @see UploadProcessingConfigurationManager#markRequestHasShallBeCancelled(String)
		 * @see UploadProcessingConfigurationManager#requestHasToBeCancelled(String))
		 * */
		private volatile boolean cancelRequest;

		/**
		 * The statistics, populated while writing the file by {@link UploadServletAsyncProcessor}
		 */
		Statistics stats = new Statistics();

		/**
		 * The desired upload rate for this request. <br>
		 * Can be null (the default rate is applied).
		 */
		private Long rateInKiloBytes;



		public Long getStats() {
			synchronized (stats) {
				// if counter is to 0
				if (stats.totalBytesWritten == 0) {
					// we dont have anything to measure rate
					return 0l;
				}
				else {

					// get the total
					// divide by the time
					long stat = stats.totalBytesWritten / (new Date().getTime() - stats.startTime) * DateUtils.MILLIS_PER_SECOND;

					// reset
					stats.reset();

					// return
					return stat;
				}

			}
		}



		class Statistics {

			private long totalBytesWritten;
			private long startTime;



			public Statistics() {
				reset();
			}


			void reset() {
				totalBytesWritten = 0;
				startTime = new Date().getTime();
			}


			void update(int numberOfBytesWritten) {
				synchronized (Statistics.this) {
					// this is the number of bytes written for the consumption of one token
					totalBytesWritten += numberOfBytesWritten;
				}
			}
		}



		public Semaphore getSemaphore() {
			return semaphore;
		}


		public Long getRateInKiloBytes() {
			return rateInKiloBytes;
		}


	}



	/**
	 * Specify that a request has to be cancelled, the file is scheduled for deletion.
	 * 
	 * @param fileId
	 * @return true if there was a pending upload for this file.
	 */
	public boolean markRequestHasShallBeCancelled(String fileId) {
		UploadProcessingConfiguration ifPresent = requestConfigMap.getIfPresent(fileId);
		if (ifPresent != null) {
			requestConfigMap.getUnchecked(fileId).cancelRequest = true;
		}
		return ifPresent != null;
	}


	public boolean requestHasToBeCancelled(String fileId) {
		return requestConfigMap.getUnchecked(fileId).cancelRequest;
	}


	public Set<Entry<String, UploadProcessingConfiguration>> getEntries() {
		return requestConfigMap.asMap().entrySet();
	}


	public void reset(String fileId) {
		final UploadProcessingConfiguration unchecked = requestConfigMap.getUnchecked(fileId);
		unchecked.semaphore = new Semaphore(0, false);
		unchecked.cancelRequest = false;
	}


	public boolean tryAcquire(String fileId) {
		return requestConfigMap.getUnchecked(fileId).semaphore.tryAcquire();
	}


	public void assignRateToRequest(String fileId, Long rateInKiloBytes) {
		requestConfigMap.getUnchecked(fileId).rateInKiloBytes = rateInKiloBytes;
	}


	public Long getRateOfRequest(String fileId) {
		return requestConfigMap.getUnchecked(fileId).rateInKiloBytes;
	}


	public Long getUploadState(String requestIdentifier) {
		UploadProcessingConfiguration config = requestConfigMap.getUnchecked(requestIdentifier);
		return config.getStats();
	}


	public UploadProcessingConfiguration getUploadProcessingConfiguration(String fileId) {
		return requestConfigMap.getUnchecked(fileId);
	}


}
