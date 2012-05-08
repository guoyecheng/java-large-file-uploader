package com.am.jlfu.fileuploader.logic;


import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.apache.commons.fileupload.FileUploadException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.exception.IncorrectRequestException;
import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.logic.UploadProcessorTest.TestFileSplitResult;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor.WriteChunkCompletionListener;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadServletAsyncProcessorTest {

	@Autowired
	UploadServletAsyncProcessor uploadServletAsyncProcessor;

	@Autowired
	UploadProcessor uploadProcessor;

	@Autowired
	RequestComponentContainer requestComponentContainer;


	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;


	MockMultipartFile file;
	String fileName = "zenameofzefile.owf";
	private Long fileSize;



	@Before
	public void init()
			throws IOException {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());


		staticStateManager.clear();
		byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
		file = new MockMultipartFile("blob", content);
		fileSize = Integer.valueOf(content.length).longValue();
	}



	private class Listener
			implements WriteChunkCompletionListener {

		private Semaphore waitForMe;



		public Listener(Semaphore waitForMe) {
			this.waitForMe = waitForMe;
		}


		@Override
		public void error(Exception exception) {
			throw new RuntimeException(exception);
		}


		@Override
		public void success() {
			waitForMe.release();
		}

	}



	@Test
	public void testInvalidCrc()
			throws IOException, IncorrectRequestException, MissingParameterException, FileUploadException, InvalidCrcException, InterruptedException {

		// begin a file upload process
		String fileId = uploadProcessor.prepareUpload(fileSize, fileName);

		// upload with bad crc
		TestFileSplitResult splitResult = UploadProcessorTest.getByteArrayFromFile(file, 0, 3);
		final Semaphore semaphore = new Semaphore(0);
		uploadServletAsyncProcessor.process(fileId, "lala", splitResult.stream, new Listener(semaphore) {

			@Override
			public void error(Exception exception) {
				Assert.assertTrue(exception instanceof InvalidCrcException);
				semaphore.release();
			}


			@Override
			public void success() {
				throw new RuntimeException();
			}

		});


		Assert.assertTrue(semaphore.tryAcquire(5, TimeUnit.SECONDS));
	}


	@Test
	public void testClassic()
			throws ServletException, IOException, InvalidCrcException, IncorrectRequestException, MissingParameterException, FileUploadException,
			InterruptedException {
		TestFileSplitResult splitResult;
		final Semaphore waitForMe = new Semaphore(0);


		// begin a file upload process
		String fileId = uploadProcessor.prepareUpload(fileSize, fileName);

		// get progress
		Assert.assertThat(0f, is(uploadProcessor.getProgress(fileId)));

		// upload first part
		splitResult = UploadProcessorTest.getByteArrayFromFile(file, 0, 3);
		uploadServletAsyncProcessor.process(fileId, splitResult.crc, splitResult.stream, new Listener(waitForMe));

		// wait until processing is done
		Assert.assertTrue(waitForMe.tryAcquire(5, TimeUnit.SECONDS));

		// get progress
		Assert.assertThat(Math.round(uploadProcessor.getProgress(fileId)), is(3 * 100 / fileSize.intValue()));

		// upload second part
		splitResult = UploadProcessorTest.getByteArrayFromFile(file, 3, 5);
		uploadServletAsyncProcessor.process(fileId, splitResult.crc, splitResult.stream, new Listener(waitForMe));

		// wait until processing is done
		Assert.assertTrue(waitForMe.tryAcquire(5, TimeUnit.SECONDS));

		// get progress
		Assert.assertThat(Math.round(uploadProcessor.getProgress(fileId)), is(Math.round(5f / fileSize.floatValue() * 100f)));

		// upload last part
		splitResult = UploadProcessorTest.getByteArrayFromFile(file, 5, fileSize.intValue());
		uploadServletAsyncProcessor.process(fileId, splitResult.crc, splitResult.stream, new Listener(waitForMe));

		// wait until processing is done
		Assert.assertTrue(waitForMe.tryAcquire(5, TimeUnit.SECONDS));

		// get progress
		Assert.assertThat(Math.round(uploadProcessor.getProgress(fileId)), is(100));
	}

}
