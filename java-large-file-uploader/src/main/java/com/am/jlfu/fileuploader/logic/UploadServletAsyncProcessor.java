package com.am.jlfu.fileuploader.logic;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.limiter.RateLimiter;
import com.am.jlfu.fileuploader.logic.UploadProcessingConfigurationManager.UploadProcessingConfiguration;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@Component
public class UploadServletAsyncProcessor {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncProcessor.class);

	@Autowired
	private RateLimiter tokenBucket;

	@Autowired
	UploadProcessingConfigurationManager uploadProcessingConfigurationManager;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	/** The executor */
	private ScheduledThreadPoolExecutor uploadWorkersPool = new ScheduledThreadPoolExecutor(1);



	public void process(String fileId, String crc, InputStream inputStream,
			WriteChunkCompletionListener completionListener)
			throws FileNotFoundException
	{

		// get entity
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();

		// extract the corresponding entity from map
		final UploadProcessingConfiguration uploadProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId);

		// if there is no configuration in the map
		if (uploadProcessingConfiguration.getRateInKiloBytes() == null) {

			// and if there is a specific configuration
			FileStateJsonBase staticFileStateJson = model.getFileStates().get(fileId).getStaticFileStateJson();
			if (staticFileStateJson != null && staticFileStateJson.getRateInKiloBytes() != null) {

				// use it
				uploadProcessingConfigurationManager.assignRateToRequest(fileId, staticFileStateJson.getRateInKiloBytes());

			}
			// otherwise
			else {

				// assign default configuration
				uploadProcessingConfigurationManager.assignRateToRequest(fileId, model.getDefaultRateInKiloBytes());

			}
		}

		// get the file
		StaticFileState fileState = model.getFileStates().get(fileId);
		if (fileState == null) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}
		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());


		// if the file does not exist, there is an issue!
		if (!file.exists()) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}

		// init crc32
		CRC32 crc32 = new CRC32();


		// initialize the streams
		OutputStream outputStream = new FileOutputStream(file, true);

		// init the task
		final WriteChunkToFileTask task =
				new WriteChunkToFileTask(fileId, uploadProcessingConfiguration, crc, inputStream, outputStream, crc32, completionListener);


		// then submit it to the workers pool
		uploadWorkersPool.submit(task);
	}



	public interface WriteChunkCompletionListener {

		public void error(Exception exception);


		public void success();
	}

	private class WriteChunkToFileTask
			implements Callable<Void> {


		private final InputStream inputStream;
		private final OutputStream outputStream;
		private final String fileId;
		private final String crc;

		private final WriteChunkCompletionListener completionListener;

		private CRC32 crc32;
		private int chunkNo;
		private UploadProcessingConfiguration uploadProcessingConfiguration;



		public WriteChunkToFileTask(String fileId, UploadProcessingConfiguration uploadProcessingConfiguration, String crc,
				InputStream inputStream,
				OutputStream outputStream, CRC32 crc32, WriteChunkCompletionListener completionListener) {
			this.fileId = fileId;
			this.uploadProcessingConfiguration = uploadProcessingConfiguration;
			this.crc = crc;
			this.inputStream = new BufferedInputStream(inputStream);
			this.outputStream = new BufferedOutputStream(outputStream);
			this.crc32 = crc32;
			this.completionListener = completionListener;
		}


		@Override
		public Void call()
				throws Exception {
			try {
				// if we can get a token
				if (uploadProcessingConfiguration.getSemaphore().tryAcquire()) {
					// process
					writeChunkWorthOneToken();
				}
				// if we cant
				else {
					// resubmit it (but wait a bit before to stress less the cpu)
					uploadWorkersPool.schedule(this, 5, TimeUnit.MILLISECONDS);
				}
			}
			// handles a stream ended unexpectedly , it just means the user has stopped the stream
			catch (IOException e) {
				if (e.getMessage().equals("Stream ended unexpectedly")) {
					log.warn("User has stopped streaming for file " + fileId);
				}
				else {
					error(e);
				}
			}
			catch (Exception e) {
				log.error("Error while sending data chunk, aborting (" + fileId + ")", e);

			}
			return null;
		}


		private void writeChunkWorthOneToken()
				throws IOException {

			// check if user wants to cancel
			if (uploadProcessingConfigurationManager.requestHasToBeCancelled(fileId)) {
				log.debug("User cancellation detected for file " + fileId + ". Closing streams now.");
				success();
				return;
			}

			// process the write for one token
			log.trace("Processing chunk {} of request ({})", chunkNo++, fileId);

			byte[] buffer = new byte[RateLimiter.SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES];

			// read from stream
			int bytesCount = inputStream.read(buffer);
			// if we have something
			if (bytesCount != -1) {

				// write it to file
				outputStream.write(buffer, 0, bytesCount);

				// and update crc32
				crc32.update(buffer, 0, bytesCount);

				// update stats
				uploadProcessingConfiguration.stats.update(bytesCount);

				// submit again
				uploadWorkersPool.submit(this);
			}
			//
			// if we are done
			else {
				log.debug("Processed part for file " + fileId + " into temp file");

				// TODO
				// compare the first chunk

				// compare the checksum of the chunks
				String calculatedChecksum = Long.toHexString(crc32.getValue());
				if (!calculatedChecksum.equals(crc)) {
					InvalidCrcException invalidCrcException = new InvalidCrcException(calculatedChecksum, crc);
					log.error(invalidCrcException.getMessage(), invalidCrcException);
					error(invalidCrcException);
					return;
				}

				// and specify as complete
				success();
			}
		}


		public void error(Exception e) {
			IOUtils.closeQuietly(outputStream);
			completionListener.error(e);
		}


		public void success() {
			IOUtils.closeQuietly(outputStream);
			completionListener.success();

		}

	}



	@ManagedAttribute
	public int getAwaitingChunks() {
		return uploadWorkersPool.getQueue().size();
	}


	public void clean(String identifier) {
		uploadProcessingConfigurationManager.reset(identifier);
	}


}
