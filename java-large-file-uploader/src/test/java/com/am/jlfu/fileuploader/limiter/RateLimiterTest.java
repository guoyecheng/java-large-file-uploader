package com.am.jlfu.fileuploader.limiter;


import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager.UploadProcessingConfiguration;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RateLimiterTest {

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;



	private void emulateUpload(Long rate, int upload, int duration)
			throws InterruptedException {
		String id = "an id";

		// init config
		final UploadProcessingConfiguration uploadProcessingConfiguration = uploadProcessingConfigurationManager.getUploadProcessingConfiguration(id);
		final long originalDownloadAllowanceForIteration = uploadProcessingConfiguration.getDownloadAllowanceForIteration();
		uploadProcessingConfigurationManager.assignRateToRequest(id, rate);
		uploadProcessingConfiguration.setProcessing(true);

		// wait for the rate modification to occur
		while (uploadProcessingConfiguration.getDownloadAllowanceForIteration() == originalDownloadAllowanceForIteration) {
		}

		// emulate an upload of a file
		long totalUpload = upload * 1024;
		final Date reference = new Date();
		long allowance;
		while (totalUpload != 0) {
			allowance = uploadProcessingConfiguration.getDownloadAllowanceForIteration();
			uploadProcessingConfiguration.bytesConsumedFromAllowance(allowance);
			totalUpload -= allowance;
		}
		long itTookThatLong = new Date().getTime() - reference.getTime();

		// specify completion
		uploadProcessingConfigurationManager.reset(id);

		// verify that it took around that duration
		Assert.assertThat(itTookThatLong, lessThan(duration + 150l));
		Assert.assertThat(itTookThatLong, greaterThan(duration - 150l));

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
