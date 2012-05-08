package com.am.jlfu.fileuploader.limiter;


import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.logic.UploadProcessingConfigurationManager;
import com.am.jlfu.fileuploader.logic.UploadProcessingConfigurationManager.UploadProcessingConfiguration;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RateLimiterTest {

	@Autowired
	UploadProcessingConfigurationManager uploadProcessingConfigurationManager;



	private void emulateUpload(Long rate, int upload, int duration)
			throws InterruptedException {
		String id = "an id";

		// init config
		final UploadProcessingConfiguration uploadProcessingConfiguration = uploadProcessingConfigurationManager.getUploadProcessingConfiguration(id);
		uploadProcessingConfigurationManager.assignRateToRequest(id, rate);

		// emulate an upload of a file
		Date reference = new Date();
		for (int i = 0; i < upload; i++) {
			Assert.assertTrue(uploadProcessingConfiguration.getSemaphore()
					.tryAcquire(10, TimeUnit.MINUTES));
		}
		long itTookThatLong = new Date().getTime() - reference.getTime();

		// specify completion
		uploadProcessingConfigurationManager.reset(id);

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
