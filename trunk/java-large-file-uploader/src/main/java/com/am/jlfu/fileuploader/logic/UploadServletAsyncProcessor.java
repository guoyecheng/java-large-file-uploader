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
import com.am.jlfu.fileuploader.limiter.RateLimiter;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@Component
public class UploadServletAsyncProcessor {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncProcessor.class);

	@Autowired
	private RateLimiter tokenBucket;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	/** The executor */
	private ScheduledThreadPoolExecutor uploadWorkersPool = new ScheduledThreadPoolExecutor(1);



	public void process(String requestIdentifier, String fileId, Long sliceFrom, String crc, InputStream inputStream,
			WriteChunkCompletionListener completionListener)
			throws FileNotFoundException
	{

		// keep it in cache
		StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();
		tokenBucket.assignConfigurationToRequest(requestIdentifier, entity.getStaticStateRateConfiguration());

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

		// init crc32
		CRC32 crc32 = new CRC32();


		// initialize the streams
		OutputStream outputStream = new FileOutputStream(file, true);

		// init the task
		final WriteChunkToFileTask task =
				new WriteChunkToFileTask(fileId, sliceFrom, crc, inputStream, outputStream, requestIdentifier, crc32, completionListener);


		// then submit it to the workers pool
		uploadWorkersPool.submit(task);
	}



	public interface WriteChunkCompletionListener {

		public void error(Exception exception);


		public void success();
	}

	private class WriteChunkToFileTask
			implements Callable<Void> {


		private final String requestIdentifier;
		private final InputStream inputStream;
		private final OutputStream outputStream;
		private final String fileId;
		private final Long sliceFrom;
		private final String crc;

		private final WriteChunkCompletionListener completionListener;

		private CRC32 crc32;
		private int chunkNo;



		public WriteChunkToFileTask(String fileId, Long sliceFrom, String crc, InputStream inputStream,
				OutputStream outputStream, String identifier, CRC32 crc32, WriteChunkCompletionListener completionListener) {
			this.requestIdentifier = identifier;
			this.fileId = fileId;
			this.sliceFrom = sliceFrom;
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
				if (tokenBucket.tryTake(requestIdentifier)) {
					// process
					writeChunkWorthOneToken();
				}
				// if we cant
				else {
					// resubmit it (but wait a bit before to stress less the cpu)
					uploadWorkersPool.schedule(this, 5, TimeUnit.MILLISECONDS);
				}
			}
			catch (Exception e) {
				log.error("Error while sending data chunk, aborting (" + fileId + ")", e);
				error(e);
			}
			return null;
		}


		private void writeChunkWorthOneToken()
				throws IOException {
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

				// submit again
				uploadWorkersPool.submit(this);
			}
			//
			// if we are done
			else {
				log.debug("Processed part " + sliceFrom + " for file " + fileId + " into temp file");

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
		tokenBucket.completed(identifier);
	}


}
