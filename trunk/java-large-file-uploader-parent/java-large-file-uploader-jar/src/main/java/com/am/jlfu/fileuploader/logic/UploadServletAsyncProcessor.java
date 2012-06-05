package com.am.jlfu.fileuploader.logic;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.limiter.RateLimiter;
import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager;
import com.am.jlfu.fileuploader.limiter.RequestUploadProcessingConfiguration;
import com.am.jlfu.fileuploader.limiter.UploadProcessingOperation;
import com.am.jlfu.fileuploader.limiter.UploadProcessingOperationManager;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=uploadServletAsyncProcessor")
public class UploadServletAsyncProcessor {

	/** The size of the buffer in bytes */
	public static final int SIZE_OF_THE_BUFFER_IN_BYTES = 8192;// 8KB

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncProcessor.class);

	@Autowired
	private RateLimiter rateLimiter;

	@Autowired
	private RateLimiterConfigurationManager uploadProcessingConfigurationManager;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	private UploadProcessingOperationManager uploadProcessingOperationManager;

	@Autowired
	private StaticStateIdentifierManager staticStateIdentifierManager;

	/** The executor that process the stream */
	private ScheduledThreadPoolExecutor uploadWorkersPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(10);



	public void process(StaticFileState fileState, UUID fileId, String crc, InputStream inputStream,
			WriteChunkCompletionListener completionListener)
			throws FileNotFoundException
	{

		// get identifier
		UUID clientId = staticStateIdentifierManager.getIdentifier();

		// extract the corresponding request entity from map
		final RequestUploadProcessingConfiguration requestUploadProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId);

		// get static file state
		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());

		// if there is no configuration in the map
		if (requestUploadProcessingConfiguration.getRateInKiloBytes() == null) {

			// and if there is a specific configuration in the file
			FileStateJsonBase staticFileStateJson = fileState.getStaticFileStateJson();
			if (staticFileStateJson != null && staticFileStateJson.getRateInKiloBytes() != null) {

				// use it
				uploadProcessingConfigurationManager.assignRateToRequest(fileId, staticFileStateJson.getRateInKiloBytes());

			}
		}


		// if the file does not exist, there is an issue!
		if (!file.exists()) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}

		// initialize the streams
		FileOutputStream outputStream = new FileOutputStream(file, true);

		// get all the processing operation
		uploadProcessingOperationManager.startOperation(clientId, fileId);
		final UploadProcessingOperation masterProcessingOperation = uploadProcessingOperationManager.getMasterProcessingOperation();
		final UploadProcessingOperation clientProcessingOperation = uploadProcessingOperationManager.getClientProcessingOperation(clientId);
		final UploadProcessingOperation requestProcessingOperation = uploadProcessingOperationManager.getFileProcessingOperation(fileId);


		// init the task
		final WriteChunkToFileTask task =
				new WriteChunkToFileTask(fileId, requestProcessingOperation, clientProcessingOperation,
						masterProcessingOperation, crc, inputStream,
						outputStream, completionListener, clientId);

		// mark the file as processing
		requestUploadProcessingConfiguration.setProcessing(true);

		// then submit the task to the workers pool
		uploadWorkersPool.submit(task);

	}



	public interface WriteChunkCompletionListener {

		public void error(Exception exception);


		public void success();
	}

	public class WriteChunkToFileTask
			implements Callable<Void> {


		private final InputStream inputStream;
		private final FileOutputStream outputStream;
		private final UUID fileId;
		private final UUID clientId;
		private final String crc;

		private final WriteChunkCompletionListener completionListener;

		private UploadProcessingOperation requestUploadProcessingOperation;
		private UploadProcessingOperation clientUploadProcessingOperation;
		private UploadProcessingOperation masterUploadProcessingOperation;

		private CRC32 crc32 = new CRC32();
		private long byteProcessed;
		private long completionTimeTakenReference;



		public WriteChunkToFileTask(UUID fileId, UploadProcessingOperation requestOperation,
				UploadProcessingOperation clientOperation, UploadProcessingOperation masterProcessingOperation,
				String crc,
				InputStream inputStream,
				FileOutputStream outputStream, WriteChunkCompletionListener completionListener, UUID clientId) {
			this.fileId = fileId;
			this.requestUploadProcessingOperation = requestOperation;
			this.clientUploadProcessingOperation = clientOperation;
			this.masterUploadProcessingOperation = masterProcessingOperation;
			this.crc = crc;
			this.inputStream = inputStream;
			this.outputStream = outputStream;
			this.completionListener = completionListener;
			this.clientId = clientId;
		}


		@Override
		public Void call()
				throws Exception {
			try {
				// if we have not exceeded our byte to write allowance
				long requestAllowance, clientAllowance, masterAllowance;
				if ((requestAllowance = requestUploadProcessingOperation.getDownloadAllowanceForIteration()) > 0 &&
						(clientAllowance = clientUploadProcessingOperation.getDownloadAllowanceForIteration()) > 0 &&
						(masterAllowance = masterUploadProcessingOperation.getDownloadAllowanceForIteration()) > 0) {

					// keep first time
					if (completionTimeTakenReference == 0) {
						completionTimeTakenReference = new Date().getTime();
						log.trace("first write " + completionTimeTakenReference);
					}

					// process
					write(minOf(
							(int) requestAllowance,
							(int) clientAllowance,
							(int) masterAllowance));
				}
				// if have exceeded it
				else {

					// by default, wait for default value
					long delay = RateLimiter.BUCKET_FILLED_EVERY_X_MILLISECONDS;


					// if we have a first write time
					if (completionTimeTakenReference != 0) {

						// calculate the delay which is basically the iteration time minus the time
						// it took to use our allowance in this iteration, so that we go directly to
						// the next iteration
						final long time = new Date().getTime();
						final long lastWriteWasAgo = time - completionTimeTakenReference;
						delay = RateLimiter.BUCKET_FILLED_EVERY_X_MILLISECONDS - lastWriteWasAgo;
						log.trace("waiting for allowance, fillbucket is expected in " + delay + "(last write was " + lastWriteWasAgo + " ago (" +
								time +
								" - " + completionTimeTakenReference + "))");
						completionTimeTakenReference = 0;
					}

					// resubmit it
					uploadWorkersPool.schedule(this, delay, TimeUnit.MILLISECONDS);
				}
			}
			catch (Exception e) {
				// forward exception
				completeWithError(e);
			}
			return null;
		}


		private void write(int available)
				throws IOException {

			// check if user wants to cancel
			if (uploadProcessingConfigurationManager.requestHasToBeCancelled(fileId)) {
				log.debug("User cancellation detected.");
				success();
				return;
			}

			// init the buffer with the size of what we read
			byte[] buffer = new byte[Math.min(available, SIZE_OF_THE_BUFFER_IN_BYTES)];

			// read from stream
			int bytesCount = inputStream.read(buffer);

			// if we have something
			if (bytesCount != -1) {

				// process the write for one token
				log.trace("Processed bytes {} of request ({})", (byteProcessed += bytesCount), fileId);

				// write it to file
				outputStream.write(buffer, 0, bytesCount);

				// and update crc32
				crc32.update(buffer, 0, bytesCount);

				// and update request allowance
				requestUploadProcessingOperation.bytesConsumedFromAllowance(bytesCount);

				// and update client allowance
				clientUploadProcessingOperation.bytesConsumedFromAllowance(bytesCount);

				// also update master allowance
				masterUploadProcessingOperation.bytesConsumedFromAllowance(bytesCount);

				// submit again
				uploadWorkersPool.submit(this);
			}
			//
			// if we are done
			else {
				String calculatedChecksum = Long.toHexString(crc32.getValue());
				log.debug("Processed part for file " + fileId + " into temp file, checking written crc " + calculatedChecksum +
						" against input crc " + crc);

				// compare the checksum of the chunks
				if (!calculatedChecksum.equals(crc)) {
					completeWithError(new InvalidCrcException(calculatedChecksum, crc));
					return;
				}

				// if the crc is valid, specify the validation to the state
				staticStateManager.setCrcBytesValidated(clientId, fileId, byteProcessed);

				// and specify as complete
				success();
			}
		}


		public void completeWithError(Exception e) {
			log.debug("error for " + fileId + ". closing file stream");
			closeFileStream();
			completionListener.error(e);
		}


		public void success() {
			log.debug("completion for " + fileId + ". closing file stream");
			closeFileStream();
			completionListener.success();
		}


		private void closeFileStream() {
			log.debug("Closing FileOutputStream of " + fileId);
			try {
				outputStream.close();
			}
			catch (Exception e) {
				log.error("Error closing file output stream for id " + fileId + ": " + e.getMessage());
			}
		}


	}



	@ManagedAttribute
	public int getAwaitingChunks() {
		return uploadWorkersPool.getQueue().size();
	}


	public void clean(UUID clientId, UUID fileId) {
		log.debug("resetting token bucket for " + fileId);

		// deleting operation
		uploadProcessingOperationManager.stopOperation(clientId, fileId);

		// resetting configuration
		uploadProcessingConfigurationManager.reset(fileId);

	}


	/**
	 * Checks whether is file is paused or not.
	 * 
	 * @param fileId
	 * @return
	 */
	public boolean isPausedOrCancelled(UUID fileId) {
		StaticStatePersistedOnFileSystemEntity entityIfPresent = staticStateManager.getEntityIfPresent();
		return entityIfPresent != null && (entityIfPresent.getFileStates().get(fileId) == null ||
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId).isPaused());
	}


	public static int minOf(int... numbers) {
		int min = -1;
		if (numbers.length > 0) {
			min = numbers[0];
			for (int i = 1; i < numbers.length; i++) {
				min = Math.min(min, numbers[i]);
			}
		}
		return min;
	}


}
