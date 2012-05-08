package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.logic.UploadProcessingConfigurationManager;
import com.am.jlfu.fileuploader.logic.UploadProcessingConfigurationManager.UploadProcessingConfiguration;



/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 22:08
 */
@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=rateLimiter")
public class RateLimiter
{

	private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

	@Autowired
	UploadProcessingConfigurationManager uploadProcessingConfigurationManager;

	/** The size of the buffer in bytes */
	public static final int SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES = 1024;// 1KB

	/** The default request capacity. volatile because it can be changed. */
	private volatile int defaultRateInBytes = 10 * 1024 * SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES;
	// 10MB PER SECOND

	/** Number of times the bucket is filled per second. */
	public static final int NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND = 10;



	@Scheduled(fixedRate = DateUtils.MILLIS_PER_SECOND / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND)
	/**
	 * This method releases from the per request map of buckets a number of semaphores depending of the rate limitation value 
	 */
	// TODO re-optimize
	// TODO master rate limitation (not just per request)
	public void fillBucket() {

		// calculate the maximum amount of semaphores to release which is gonna limit the rate.
		// basically: the rate we want divided by the amount of data a bucket represent and divided
		// by the number of times the bucket is filled (because
		// we pass in this method that often).

		// for all the requests we have
		for (Entry<String, UploadProcessingConfiguration> count : uploadProcessingConfigurationManager.getEntries()) {

			// if it is paused, we do nothing.. The bucket is gonna get exhausted pretty soon :)
			if (!count.getValue().isPaused()) {

				// calculate from the rate in the config
				int requestBucketCapacity;
				Long rateInKiloBytes = count.getValue().getRateInKiloBytes();
				if (rateInKiloBytes != null) {

					// check min
					if (rateInKiloBytes < 10) {
						rateInKiloBytes = 10l;
					}

					// then calculate
					requestBucketCapacity = (int) (rateInKiloBytes * 1024 / SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES);

				}
				else {
					requestBucketCapacity = defaultRateInBytes / SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES;
				}

				// then calculate the maximum number to release;
				final int maxToRelease = requestBucketCapacity / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;

				// release maximum minus number of semaphores still present
				final int releaseCount = maxToRelease - count.getValue().getSemaphore().availablePermits();

				// if we have consumed some
				if (releaseCount > 0) {
					// release
					count.getValue().getSemaphore().release(releaseCount);
					log.trace("Releasing {} tokens for request #{}, available tokens: " + count.getValue().getSemaphore()
							.availablePermits(), releaseCount,
							count.getKey());
				}
			}
		}
	}


	@ManagedAttribute
	public int getEachBucketCapacity() {
		return defaultRateInBytes;
	}


	@ManagedAttribute
	public void setEachBucketCapacity(int eachBucketCapacity) {
		Validate.isTrue(eachBucketCapacity >= 0);
		this.defaultRateInBytes = eachBucketCapacity;
	}

}
