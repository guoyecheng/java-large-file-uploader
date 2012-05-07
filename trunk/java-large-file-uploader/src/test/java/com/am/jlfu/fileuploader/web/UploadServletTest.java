package com.am.jlfu.fileuploader.web;


import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.util.Map;

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

import com.am.jlfu.fileuploader.json.FileStateJson;
import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.google.gson.Gson;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadServletTest {

	@Autowired
	UploadServlet uploadServlet;

	@Autowired
	UploadServletAsync uploadServletAsync;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	MockHttpServletRequest request;
	MockHttpServletResponse response;

	private byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
	private MockMultipartFile file = new MockMultipartFile("blob", content);



	@Before
	public void init() {

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		// populate request component container
		requestComponentContainer.populate(request, response);

	}


	@Test
	public void getConfig()
			throws IOException {

		// init an upload to emulate a pending file
		String fileId = prepareUpload();

		// set action parameter
		request.clearAttributes();
		response = new MockHttpServletResponse();
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getConfig.name());

		// handle request
		uploadServlet.handleRequest(request, response);

		// extract config from response
		InitializationConfiguration fromJson = new Gson().fromJson(response.getContentAsString(), InitializationConfiguration.class);
		Assert.assertNotNull(fromJson.getInByte());
		Assert.assertThat(response.getStatus(), is(200));
		Map<String, FileStateJson> pendingFiles = fromJson.getPendingFiles();
		Assert.assertThat(pendingFiles.size(), is(1));
		Assert.assertThat(pendingFiles.keySet().iterator().next(), is(fileId));
	}


	@Test
	public void getProgressWithBadId()
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getProgress.name());
		String id = "a bad id";
		request.setParameter(UploadServletParameter.fileId.name(), id);

		// handle request
		uploadServlet.handleRequest(request, response);
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);
		Assert.assertThat("File with id " + id + " not found", is(fromJson.getValue()));
	}


	@Test
	public void getProgress()
			throws IOException {

		// init an upload to emulate a pending file
		String fileId = prepareUpload();

		// set action parameter
		request.clearAttributes();
		response = new MockHttpServletResponse();
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getProgress.name());
		request.setParameter(UploadServletParameter.fileId.name(), fileId);

		// handle request
		uploadServlet.handleRequest(request, response);
		Assert.assertThat(response.getStatus(), is(200));
		ProgressJson fromJson = new Gson().fromJson(response.getContentAsString(), ProgressJson.class);
		Assert.assertThat(Float.valueOf(fromJson.getProgress()), is(Float.valueOf(0)));

	}


	@Test
	public void uploadNotMultipartParams()
			throws IOException, ServletException {

		// handle request
		uploadServletAsync.handleRequest(request, response);
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);
		Assert.assertThat("Request should be multipart", is(fromJson.getValue()));

	}


	@Test
	public void prepareUploadTest()
			throws IOException {
		prepareUpload();
	}


	public String prepareUpload()
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.prepareUpload.name());
		request.setParameter(UploadServletParameter.fileName.name(), "jambon");
		request.setParameter(UploadServletParameter.size.name(), content.length + "");

		// handle request
		uploadServlet.handleRequest(request, response);
		Assert.assertThat(response.getStatus(), is(200));
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);

		return fromJson.getValue();

	}


	// upload,
	// prepareUpload,
	// clearFile,
	// clearAll;


	@Test
	public void clearFileWithMissingParameter()
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.clearFile.name());

		// handle request
		uploadServlet.handleRequest(request, response);

		// assert that we have an error
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);

		Assert.assertThat("The parameter fileId is missing for this request.", is(fromJson.getValue()));

	}


	// @Test
	// public void completeTest()
	// throws IOException, ServletException {
	//
	//
	// // prepare upload
	// String fileId = prepareUpload();
	//
	//
	// // populate request component container with multipart request
	// MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
	// requestComponentContainer.populate(multipartRequest, response);
	//
	// // populate multipart request session with the previous call to keep the identifier
	// multipartRequest.setSession(request.getSession());
	//
	// // set the file to request
	// FilePart[] parts = new FilePart[] {
	// new FilePart("name", new ByteArrayPartSource("i am a file", content)) };
	// MultipartRequestEntity multipartRequestEntity =
	// new MultipartRequestEntity(parts, new PostMethod().getParams());
	// ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
	// multipartRequestEntity.writeRequest(requestContent);
	// multipartRequest.setContent(requestContent.toByteArray());
	// multipartRequest.setContentType(multipartRequestEntity.getContentType());
	//
	// // get crc
	// CRC32 crc32 = new CRC32();
	// crc32.update(content);
	//
	// // set action parameters
	// multipartRequest.setParameter(UploadServletParameter.fileId.name(), fileId);
	// multipartRequest.setParameter(UploadServletParameter.sliceFrom.name(), 0 + "");
	// multipartRequest.setParameter(UploadServletParameter.crc.name(),
	// Long.toHexString(crc32.getValue()));
	//
	// // handle request
	// response = new MockHttpServletResponse();
	// uploadServletAsync.handleRequest(multipartRequest, response);
	// Assert.assertThat(response.getStatus(), is(200));
	//
	// }

}
