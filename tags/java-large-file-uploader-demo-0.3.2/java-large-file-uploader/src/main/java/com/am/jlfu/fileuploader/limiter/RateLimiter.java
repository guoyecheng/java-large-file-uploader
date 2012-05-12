package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager.UploadProcessingConfiguration;



@Component
public class RateLimiter
{


	private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;


	/** Number of times the bucket is filled per second. */
	public static final int NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND = 10;



	@Scheduled(fixedRate = DateUtils.MILLIS_PER_SECOND / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND)
	// TODO re-optimize
	// TODO master rate limitation and per client (not just per request)
	// TODO expose configuration to jmx
	public void fillBucket() {

		// for all the requests we have
		for (Entry<String, UploadProcessingConfiguration> count : uploadProcessingConfigurationManager.getEntries()) {

			// if it is paused, we do not refill the bucket
			if (!count.getValue().isPaused()) {

				// default per default
				long allowedCapacityPerSecond = uploadProcessingConfigurationManager.getDefaultRatePerRequestInKiloBytes() * 1024;

				// calculate from the rate in the config
				Long rateInKiloBytes = count.getValue().getRateInKiloBytes();
				if (rateInKiloBytes != null) {

					// check min
					if (rateInKiloBytes < uploadProcessingConfigurationManager.getMinimumRatePerRequestInKiloBytes()) {
						rateInKiloBytes = uploadProcessingConfigurationManager.getMinimumRatePerRequestInKiloBytes();
					}

					// then calculate
					allowedCapacityPerSecond = (int) (rateInKiloBytes * 1024);

				}

				// calculate what we can write per iteration
				final long allowedCapacityPerIteration = allowedCapacityPerSecond / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;

				// calculate statistics from what has been consumed in the iteration
				count.getValue().setInstantRateInBytes(allowedCapacityPerSecond - count.getValue().getDownloadAllowanceForIteration());

				// set it to the rate conf element
				count.getValue().setDownloadAllowanceForIteration(allowedCapacityPerIteration);

				log.trace("giving an allowance of " + allowedCapacityPerIteration + " bytes to " + count.getKey() + ". (consumed " +
						count.getValue().getInstantRateInBytes() + " bytes during previous iteration)");
			}
		}
	}

}
