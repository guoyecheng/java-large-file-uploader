package com.am.jlfu.fileuploader.limiter;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 22:08
 */
@Component
@ManagedResource
public class PerRequestTokenBucket implements TokenBucket {

	private static final Logger log = LoggerFactory.getLogger(PerRequestTokenBucket.class);

	private final LoadingCache<Long, Semaphore> bucketSizeByRequestNo = CacheBuilder.newBuilder().build(new CacheLoader<Long, Semaphore>() {
		@Override
		public Semaphore load(Long arg0) throws Exception {
			log.trace("Created new bucket for #{}", arg0);
			return new Semaphore(0, false);
		}
	});

	//10MB PER SECOND
	
	private volatile int eachBucketCapacity = 100 * 1024 / SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES;

	public static final int NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND = 10;

	@Scheduled(fixedRate = DateUtils.MILLIS_PER_SECOND / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND)
	public void fillBucket() {
		final int maxToRelease = eachBucketCapacity / NUMBER_OF_TIMES_THE_BUCKET_IS_FILLED_PER_SECOND;
		log.debug("Releasing up to {} tokens, total stored counts: {}", maxToRelease, bucketSizeByRequestNo.size());
		for (Map.Entry<Long, Semaphore> count : bucketSizeByRequestNo.asMap().entrySet()) {
			final int releaseCount = Math.min(maxToRelease, eachBucketCapacity - count.getValue().availablePermits());
			if (releaseCount > 0) {
				count.getValue().release(releaseCount);
				log.trace("Releasing {} tokens for request #{}", releaseCount, count.getKey());
			}
		}
	}


	private Long getRequestNo(ServletRequest req) {
		final Long reqNo = (Long) req.getAttribute(REQUEST_NO);
		if (reqNo == null) {
			throw new IllegalAccessError("Request # not found in: " + req);
		}
		return reqNo;
	}

	@Override
	public boolean tryTake(ServletRequest req) {
		return bucketSizeByRequestNo.getUnchecked(getRequestNo(req)).tryAcquire();
	}

	@Override
	public void completed(ServletRequest req) {
		bucketSizeByRequestNo.invalidate(getRequestNo(req));
		log.trace("Completed #{}, destroying bucket", getRequestNo(req));
	}

	@ManagedAttribute
	public int getEachBucketCapacity() {
		return eachBucketCapacity;
	}

	@ManagedAttribute
	public void setEachBucketCapacity(int eachBucketCapacity) {
		Validate.isTrue(eachBucketCapacity >= 0);
		this.eachBucketCapacity = eachBucketCapacity;
	}

	@ManagedAttribute
	public long getOngoingRequests() {
		return bucketSizeByRequestNo.size();
	}
}