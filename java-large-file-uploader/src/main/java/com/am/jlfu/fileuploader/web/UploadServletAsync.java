package com.am.jlfu.fileuploader.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.am.jlfu.fileuploader.exception.IncorrectRequestException;
import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.limiter.TokenBucket;
import com.am.jlfu.fileuploader.logic.UploadProcessor;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;

@Component("javaLargeFileUploaderAsyncServlet")
@WebServlet(name = "javaLargeFileUploaderAsyncServlet", urlPatterns = { "/javaLargeFileUploaderAsyncServlet" }, asyncSupported = true)
public class UploadServletAsync extends HttpRequestHandlerServlet implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsync.class);
	
	
	@Autowired
	private TokenBucket tokenBucket;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	/** The executor */
	ScheduledThreadPoolExecutor uploadWorkersPool = new ScheduledThreadPoolExecutor(1);

	/**
	 * Maximum time that a streaming request can take.
	 */
	//TODO make configurable
	private long taskTimeOut = DateUtils.MILLIS_PER_HOUR;
	
	/** Requests identifier counter. */
	private AtomicLong requestNo = new AtomicLong();

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		//process the request
		try {
			processUpload(request, response);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			UploadServlet.writeExceptionToResponse(e, response);
		}

	}
	

	public void processUpload(HttpServletRequest req, HttpServletResponse response)
			throws IOException, IncorrectRequestException, MissingParameterException, FileUploadException, InvalidCrcException {


		// check if the request is multipart:
		if (!ServletFileUpload.isMultipartContent(req)) {
			throw new IncorrectRequestException("Request should be multipart");
		}

		// extract the fields
		String fileId = UploadServlet.getParameterValue(req, UploadServletParameter.fileId);
		Long sliceFrom = Long.valueOf(UploadServlet.getParameterValue(req, UploadServletParameter.sliceFrom));
		String crc = UploadServlet.getParameterValue(req, UploadServletParameter.crc);
		

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		// parse the request
		FileItemIterator iter = upload.getItemIterator(req);
		FileItemStream item = iter.next();

		// throw exception if item is null
		if (item == null) {
			throw new IncorrectRequestException("No file to upload found in the request");
		}

		//get a request number
		long curRequestNo = requestNo.incrementAndGet();
		//assign it to the request
		req.setAttribute(TokenBucket.REQUEST_NO, curRequestNo);
		log.info("Serving: {} ({})", req.getRequestURI(), curRequestNo);

		
		
		// get the file
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();
		StaticFileState fileState = model.getFileStates().get(fileId);
		if (fileState == null) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}
		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());


		// if the file does not exist, there is an issue!
		if (!file.exists()) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}
		
		//init crc32
		CRC32 crc32 = new CRC32();

		//process the request asynchronously
		final AsyncContext asyncContext = req.startAsync(req, response);
		asyncContext.setTimeout(taskTimeOut);

		//initialize the streams
		InputStream inputStream = item.openStream();
		OutputStream outputStream = new FileOutputStream(file, true);

		//init the task
		final WriteChunkToFileTask task = new WriteChunkToFileTask(asyncContext, fileId, sliceFrom, crc, inputStream, outputStream, file, curRequestNo, crc32);
		
		//add the task as a listener as it manages the inputstream
		asyncContext.addListener(task);
		
		//then submit it to the workers pool
		uploadWorkersPool.submit(task);
	}

	

	private class WriteChunkToFileTask implements Callable<Void>, AsyncListener {

		
		private InputStream inputStream;
		private OutputStream outputStream;
		private AsyncContext ctx;
		private long requestNo;
		private String fileId;
		private Long sliceFrom;
		private CRC32 crc32;
		int chunkNo;
		private String crc;


		public WriteChunkToFileTask(AsyncContext asyncContext, String fileId, Long sliceFrom, String crc, InputStream inputStream, OutputStream outputStream, File file, long curRequestNo, CRC32 crc32) {
			this.ctx = asyncContext;
			this.requestNo = curRequestNo;
			this.fileId=fileId;
			this.sliceFrom=sliceFrom;
			this.crc=crc;
			this.inputStream=new BufferedInputStream(inputStream);
			this.outputStream = new BufferedOutputStream(outputStream);
			this.crc32 = crc32;
		}

		@Override
		public Void call() throws Exception {
			try {
				//if we can get a token
				if (tokenBucket.tryTake(ctx.getRequest())) {
					//process 
					sendChunkWorthOneToken();
				} 
				//if we cant
				else {
					//resubmit it (but wait a bit before to stress less the cpu)
					uploadWorkersPool.schedule(this, 100, TimeUnit.MILLISECONDS);
				}
			} catch (Exception e) {
				log.error("Error while sending data chunk, aborting (" + requestNo + ")", e);
				ctx.complete();
			}
			return null;
		}

		private void sendChunkWorthOneToken() throws IOException {
			//process the write for one token
			log.trace("Processing chunk {} of request ({})", chunkNo++, requestNo);

			byte[] buffer = new byte[TokenBucket.SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES];

			//read from stream 
			int bytesCount = inputStream.read(buffer);
			//if we have something
			if (bytesCount != -1) {

				//write it to file
				outputStream.write(buffer, 0, bytesCount);
				
				//and update crc32
				crc32.update(buffer, 0, bytesCount);
				
				//release and submit again
				relaseSemaphore();
				uploadWorkersPool.submit(this);
			}
			//
			//if we are done
			else {
				log.debug("Processed part " + sliceFrom + " for file " + fileId + " into temp file");

				// TODO
				// compare the first chunk

				//compare the checksum of the chunks
				String calculatedChecksum = Long.toHexString(crc32.getValue());
				if (!calculatedChecksum.equals(crc)) {
					InvalidCrcException invalidCrcException = new InvalidCrcException(calculatedChecksum, crc);
					log.error(invalidCrcException.getMessage(), invalidCrcException);
					UploadServlet.writeExceptionToResponse(invalidCrcException, ctx.getResponse());
				}
				
				//and specify as complete
				ctx.complete();
			} 
			
			
		}
		
		private void relaseSemaphore() {
			tokenBucket.completed(ctx.getRequest());
		}

		@Override
		public void onComplete(AsyncEvent asyncEvent) throws IOException {
			inputStream.close();
			relaseSemaphore();
			log.debug("Done: ({})", requestNo);
		}

		@Override
		public void onTimeout(AsyncEvent asyncEvent) throws IOException {
			log.warn("Asynchronous request timeout ({})", requestNo);
			onComplete(asyncEvent);
		}

		@Override
		public void onError(AsyncEvent asyncEvent) throws IOException {
			log.warn("Asynchronous request error (" + requestNo + ")", asyncEvent.getThrowable());
			onComplete(asyncEvent);
		}

		@Override
		public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
		}
	}

	@ManagedAttribute
	public int getAwaitingChunks() {
		return uploadWorkersPool.getQueue().size();
	}
	



}
