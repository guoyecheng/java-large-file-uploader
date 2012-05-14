package com.am.jlfu.fileuploader.limiter;


import java.util.Map.Entry;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;




@Component
public class RateLimiter
{


	private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;


	/** Number of times the bucket is filled per second. */
	public static final int NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND = 10;



	@Scheduled(fixedRate = DateUtils.MILLIS_PER_SECOND / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND)
	// TODO per client limitation (not just per request)
	public void fillBucket() {

		//TODO use a map for all the clients also

		//first we need to calculate how many uploads are currently being processed
		int uploadsBeingProcessed = 0;
		for (Entry<String, UploadProcessingConfiguration> entry : uploadProcessingConfigurationManager.getEntries()) {
			uploadsBeingProcessed += entry.getValue().isProcessing() ? 1 : 0;
		}
		log.trace("refilling the upload allowance of the "+uploadsBeingProcessed+" uploads being processed");
		
		//if we have entries
		if (uploadsBeingProcessed > 0) {

			//calculate maximum limitation
			//and assign it 
			uploadProcessingConfigurationManager.getMasterProcessingConfiguration().setDownloadAllowanceForIteration(uploadProcessingConfigurationManager.getMaximumOverAllRateInKiloBytes() * 1024 / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND);
			
			
			// for all the requests we have
			for (Entry<String, UploadProcessingConfiguration> count : uploadProcessingConfigurationManager.getEntries()) {
	
				// if it is paused, we do not refill the bucket
				if (count.getValue().isProcessing()) {
	
					// default per request
					long allowedCapacityPerSecond = uploadProcessingConfigurationManager.getDefaultRatePerRequestInKiloBytes() * 1024;
	
					// calculate from the rate in the config
					Long rateInKiloBytes = count.getValue().getRateInKiloBytes();
					if (rateInKiloBytes != null) {
						allowedCapacityPerSecond = (int) (rateInKiloBytes * 1024);
					}
	
					// calculate statistics from what has been consumed in the iteration
					count.getValue().setInstantRateInBytes(count.getValue().getAndResetBytesWritten() * NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND);
	
					// calculate what we can write per iteration
					final long allowedCapacityPerIteration = allowedCapacityPerSecond / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;
	
					// set it to the rate conf element
					count.getValue().setDownloadAllowanceForIteration(allowedCapacityPerIteration);
	
					log.trace("giving an allowance of " + allowedCapacityPerIteration + " bytes to " + count.getKey() + ". (consumed " +
							count.getValue().getInstantRateInBytes() / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND + " bytes during previous iteration)");
				}
			}
		
		}
	}

}
