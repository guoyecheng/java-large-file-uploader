package com.am.jlfu.fileuploader.logic;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.zip.CRC32;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.utils.CRCHelper;
import com.am.jlfu.fileuploader.utils.CRCHelper.CRCResult;
import com.am.jlfu.fileuploader.web.UploadServletAsync;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadProcessorTest {

	private static final Logger log = LoggerFactory.getLogger(UploadProcessorTest.class);

	@Autowired
	CRCHelper crcHelper;

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

	private byte[] content;



	@Before
	public void init()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());


		staticStateManager.clear();
		fileId = null;
		content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
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



	public static TestFileSplitResult getByteArrayFromInputStream(InputStream inputStream, long start, long length)
			throws IOException {
		TestFileSplitResult testFileSplitResult = new TestFileSplitResult();

		inputStream.skip(start);

		// read file
		byte[] b = new byte[Math.min((int) (length - start), inputStream.available())];
		inputStream.read(b, 0, b.length);
		inputStream.close();
		testFileSplitResult.stream = new ByteArrayInputStream(b);

		// get crc
		CRC32 crc32 = new CRC32();
		crc32.update(b);
		testFileSplitResult.crc = Long.toHexString(crc32.getValue());

		return testFileSplitResult;
	}


	public static TestFileSplitResult getByteArrayFromFile(MultipartFile file2, long start, long length)
			throws IOException {
		InputStream inputStream = file2.getInputStream();
		return getByteArrayFromInputStream(inputStream, start, length);
	}


	@Test
	public void testCancelFileUpload()
			throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {

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


	@Test
	public void testCrcBuffered()
			throws IOException {

		// with method
		CRCResult withMethod = crcHelper.getBufferedCrc(file.getInputStream());

		// without buffer
		CRC32 crc32 = new CRC32();
		crc32.update(IOUtils.toByteArray(file.getInputStream()));
		String hexString = Long.toHexString(crc32.getValue());

		Assert.assertThat(withMethod.getCrcAsString(), is(hexString));
		Assert.assertThat(withMethod.getTotalRead(), is(content.length));

	}
}
