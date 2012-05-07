package com.am.jlfu.fileuploader.logic;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import javax.servlet.ServletException;

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

import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.web.UploadServletAsync;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadProcessorTest {


	@Autowired
	UploadProcessor uploadProcessor;

	@Autowired
	UploadServletAsync uploadServletAsync;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	MockMultipartFile file;

	String fileName = "zenameofzefile.owf";

	private String fileId;

	private Long fileSize;



	@Before
	public void init()
			throws IOException {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());


		staticStateManager.clear();
		fileId = null;
		byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
		file = new MockMultipartFile("blob", content);
		fileSize = Integer.valueOf(content.length).longValue();
	}


	@Test
	public void progressCalculationTest() {
		// check basic 30% (30/100)
		Assert.assertThat(Double.valueOf(30), is(uploadProcessor.getProgress(30l, 100l)));
		Long bigValue = 1000000000000000000l;
		// check that we dont return 100% if values are not exactly equals
		Assert.assertThat(Double.valueOf(100), is(not(uploadProcessor.getProgress(bigValue - 1, bigValue))));
		// check that we return 100% if values are equals
		Assert.assertThat(Double.valueOf(100), is(uploadProcessor.getProgress(bigValue, bigValue)));
		// check that we return 0% when 0/x
		Assert.assertThat(Double.valueOf(0), is(uploadProcessor.getProgress(0l, 240l)));
	}


	private void assertState(StaticFileState state, boolean absolutePathOfUploadedFileFilled, Boolean fileComplete, String originalFileName,
			Long fileSize,
			Long completion) {
		Assert.assertNotNull(state);
		Assert.assertNotNull(state.getStaticFileStateJson());
		if (absolutePathOfUploadedFileFilled) {
			Assert.assertNotNull(state.getAbsoluteFullPathOfUploadedFile());
		}
		else {
			Assert.assertNull(state.getAbsoluteFullPathOfUploadedFile());
		}
		Assert.assertEquals(fileName, state.getStaticFileStateJson().getOriginalFileName());
		Assert.assertEquals(fileSize, state.getStaticFileStateJson().getOriginalFileSizeInBytes());
	}



	public static class TestFileSplitResult {

		ByteArrayInputStream stream;
		String crc;
	}



	public static TestFileSplitResult getByteArrayFromFile(MockMultipartFile file, int start, int length)
			throws IOException {
		TestFileSplitResult testFileSplitResult = new TestFileSplitResult();

		// read file
		byte[] b = new byte[length - start];
		InputStream inputStream = file.getInputStream();
		inputStream.skip(start);
		inputStream.read(b, 0, b.length);
		inputStream.close();
		testFileSplitResult.stream = new ByteArrayInputStream(b);

		// get crc
		CRC32 crc32 = new CRC32();
		crc32.update(b);
		testFileSplitResult.crc = Long.toHexString(crc32.getValue());

		return testFileSplitResult;
	}


	@Test
	public void testCancelFileUpload()
			throws ServletException, IOException {

		// begin a file upload process
		String fileId = uploadProcessor.prepareUpload(fileSize, fileName);

		// assert that the state has what we want
		StaticFileState value = staticStateManager.getEntity().getFileStates().get(fileId);
		assertState(value, true, false, fileName, fileSize, 0l);

		// assert that we have it in the pending files
		Assert.assertThat(uploadProcessor.getConfig().getPendingFiles().containsKey(fileId), is(true));

		// cancel
		uploadProcessor.clearFile(fileId);

		// assert that file is reset
		Assert.assertThat(staticStateManager.getEntity().getFileStates().containsKey(fileId), is(false));

		// assert that we dont have it in the pending files anymore
		Assert.assertThat(uploadProcessor.getConfig().getPendingFiles().containsKey(fileId), is(false));
	}


	@Test
	public void testConfig()
			throws IOException {
		InitializationConfiguration config = uploadProcessor.getConfig();
		Assert.assertNotNull(config.getInByte());
	}

}
