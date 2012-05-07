package com.am.jlfu.fileuploader.limiter;


import static org.hamcrest.CoreMatchers.is;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.limiter.RateLimiter.RequestConfig;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RateLimiterTest {

	@Autowired
	RateLimiter rateLimiter;



	private void emulateUpload(Long rate, int upload, int duration)
			throws InterruptedException {
		String id = "an id";

		// init config
		RequestConfig unchecked = rateLimiter.requestConfigMap.getUnchecked(id);
		unchecked.rateInKiloBytes = rate;

		// emulate an upload of a file
		Date reference = new Date();
		for (int i = 0; i < upload; i++) {
			Assert.assertTrue(unchecked.semaphore.tryAcquire(1, TimeUnit.MINUTES));
		}
		long itTookThatLong = new Date().getTime() - reference.getTime();

		// get stat
		long currentRateInKiloBytes = unchecked.getStats() / 1024;
		Assert.assertThat(currentRateInKiloBytes, is(rate));

		// specify completion
		rateLimiter.completed(id);

		// verify that it took around that duration
		Assert.assertTrue(itTookThatLong < duration + 100);
		Assert.assertTrue(itTookThatLong > duration - 100);

	}


	@Test
	public void testWithOneClientRateModification()
			throws ExecutionException, InterruptedException {

		// lets say we upload a 10kb file.
		int upload = 10;

		// set rate to 10kb, should have taken 1 second
		emulateUpload(10l, upload, 1000);

		// set rate to 20kb, should have taken 0.5second
		emulateUpload(20l, upload, 500);

		// set rate to 50kb, should have taken 0.2 seconds
		emulateUpload(50l, upload, 200);
	}


	public void testWithTwoClients() {

	}


}
