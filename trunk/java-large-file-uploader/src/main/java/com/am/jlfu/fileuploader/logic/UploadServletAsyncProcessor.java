package com.am.jlfu.fileuploader.logic;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager;
import com.am.jlfu.fileuploader.limiter.UploadProcessingConfiguration;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@Component
public class UploadServletAsyncProcessor {

	/** The size of the buffer in bytes */
	public static final int SIZE_OF_THE_BUFFER_IN_BYTES = 8192;// 8KB

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncProcessor.class);

	@Autowired
	RateLimiterConfigurationManager uploadProcessingConfigurationManager;

	@Autowired
	private StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	private StaticStateIdentifierManager staticStateIdentifierManager;

	/** The executor that writes the file */
	private ThreadPoolExecutor uploadWorkersPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);



	public void process(String fileId, String crc, InputStream inputStream,
			WriteChunkCompletionListener completionListener)
			throws FileNotFoundException
	{

		// get identifier
		String clientId = staticStateIdentifierManager.getIdentifier();

		// get entity
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();

		// extract the corresponding entity from map
		final UploadProcessingConfiguration uploadProcessingConfiguration =
				uploadProcessingConfigurationManager.getUploadProcessingConfiguration(fileId);

		// if there is no configuration in the map
		if (uploadProcessingConfiguration.getRateInKiloBytes() == null) {

			// and if there is a specific configuration in th file
			FileStateJsonBase staticFileStateJson = model.getFileStates().get(fileId).getStaticFileStateJson();
			if (staticFileStateJson != null && staticFileStateJson.getRateInKiloBytes() != null) {

				// use it
				uploadProcessingConfigurationManager.assignRateToRequest(fileId, staticFileStateJson.getRateInKiloBytes());

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

		// initialize the streams
		FileOutputStream outputStream = new FileOutputStream(file, true);

		// init the task
		final WriteChunkToFileTask task =
				new WriteChunkToFileTask(fileId, uploadProcessingConfiguration, crc, inputStream, outputStream, completionListener, clientId);

		// mark the file as processing
		uploadProcessingConfiguration.setProcessing(true);

		// then submit the task to the workers pool
		uploadWorkersPool.submit(task);

	}



	public interface WriteChunkCompletionListener {

		public void error(Exception exception);


		public void success();
	}

	private class WriteChunkToFileTask
			implements Callable<Void> {


		private final InputStream inputStream;
		private final FileOutputStream outputStream;
		private final String fileId;
		private final String clientId;
		private final String crc;

		private final WriteChunkCompletionListener completionListener;

		private UploadProcessingConfiguration uploadProcessingConfiguration;

		private CRC32 crc32 = new CRC32();
		private long byteProcessed;



		public WriteChunkToFileTask(String fileId, UploadProcessingConfiguration uploadProcessingConfiguration, String crc,
				InputStream inputStream,
				FileOutputStream outputStream, WriteChunkCompletionListener completionListener, String clientId) {
			this.fileId = fileId;
			this.uploadProcessingConfiguration = uploadProcessingConfiguration;
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
				if (uploadProcessingConfiguration.getDownloadAllowanceForIteration() > 0 &&
						uploadProcessingConfigurationManager.getMasterProcessingConfiguration().getDownloadAllowanceForIteration() > 0) {
					// process
					write();
				}
				// if we cant
				else {
					// resubmit it
					uploadWorkersPool.submit(this);
				}
			}
			catch (Exception e) {
				// forward exception
				completeWithError(e);
			}
			return null;
		}


		private void write()
				throws IOException {

			// check if user wants to cancel
			if (uploadProcessingConfigurationManager.requestHasToBeCancelled(fileId)) {
				log.debug("User cancellation detected.");
				success();
				return;
			}

			// define the size of what we read
			int min = Math.min((int) uploadProcessingConfiguration.getDownloadAllowanceForIteration(), SIZE_OF_THE_BUFFER_IN_BYTES);
			min = (int) Math.min(uploadProcessingConfigurationManager.getMasterProcessingConfiguration().getDownloadAllowanceForIteration(), min);
			byte[] buffer = new byte[min];

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

				// and update allowance
				uploadProcessingConfiguration.bytesConsumedFromAllowance(bytesCount);

				// also update master allowance
				uploadProcessingConfigurationManager.getMasterProcessingConfiguration().bytesConsumedFromAllowance(bytesCount);

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
			clean(fileId);
			completionListener.error(e);
		}


		public void success() {
			log.debug("completion for " + fileId + ". closing file stream");
			closeFileStream();
			clean(fileId);
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


	public void clean(String identifier) {
		log.debug("resetting token bucket for " + identifier);
		uploadProcessingConfigurationManager.reset(identifier);
	}


}
