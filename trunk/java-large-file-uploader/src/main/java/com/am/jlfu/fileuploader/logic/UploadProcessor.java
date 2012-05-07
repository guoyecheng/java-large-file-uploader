package com.am.jlfu.fileuploader.logic;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.json.FileStateJson;
import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.limiter.RateLimiter;
import com.am.jlfu.fileuploader.utils.UploadLockMap;
import com.am.jlfu.staticstate.StaticStateDirectoryManager;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.google.common.base.Function;
import com.google.common.collect.Maps;



@Component
public class UploadProcessor {

	private static final Logger log = LoggerFactory.getLogger(UploadProcessor.class);

	@Autowired
	RateLimiter rateLimiter;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	StaticStateDirectoryManager staticStateDirectoryManager;

	@Autowired
	UploadLockMap lockMap;
	
	/** Executor service for closing streams */
	private ExecutorService streamCloserExecutor = Executors.newSingleThreadExecutor();

	/**
	 * Size of a slice <br>
	 * Default to 1MB.
	 */
	// Float sliceSize = 1048576f;
	// Float sliceSize = 104857600f; // 100mb
	Float sliceSize = 10485760f; // 10mb



	public InitializationConfiguration getConfig() {
		InitializationConfiguration config = new InitializationConfiguration();
		StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();

		// fill pending files from static state
		if (entity != null) {
			config.setPendingFiles(Maps.transformValues(entity.getFileStates(), new Function<StaticFileState, FileStateJson>() {

				@Override
				public FileStateJson apply(StaticFileState value) {
					File file = new File(value.getAbsoluteFullPathOfUploadedFile());
					Long fileSize = file.length();
					FileStateJsonBase staticFileStateJson = value.getStaticFileStateJson();
					FileStateJson fileStateJson = new FileStateJson();
					fileStateJson.setFileComplete(fileSize.equals(staticFileStateJson.getOriginalFileSizeInBytes()));
					fileStateJson.setFileCompletionInBytes(fileSize);
					fileStateJson.setOriginalFileName(staticFileStateJson.getOriginalFileName());
					fileStateJson.setOriginalFileSizeInBytes(staticFileStateJson.getOriginalFileSizeInBytes());
					fileStateJson.setRateInKiloBytes(staticFileStateJson.getRateInKiloBytes());
					return fileStateJson;
				}


			}));
		}

		// fill configuration
		config.setInByte(sliceSize);

		return config;
	}


	public String prepareUpload(Long size, String fileName)
			throws IOException {

		// retrieve model
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();

		// extract the extension of the filename
		String fileExtension = extractExtensionOfFileName(fileName);

		// create a new file for it
		String fileId = UUID.randomUUID().toString();
		File file = new File(staticStateDirectoryManager.getUUIDFileParent(), fileId + fileExtension);
		file.createNewFile();
		StaticFileState fileState = new StaticFileState();
		FileStateJsonBase jsonFileState = new FileStateJsonBase();
		fileState.setStaticFileStateJson(jsonFileState);
		fileState.setAbsoluteFullPathOfUploadedFile(file.getAbsolutePath());
		model.getFileStates().put(fileId, fileState);

		// add info to the state
		jsonFileState.setOriginalFileName(fileName);
		jsonFileState.setOriginalFileSizeInBytes(size);

		// and returns the file identifier
		log.debug("File prepared for client " + staticStateIdentifierManager.getIdentifier() + " at path " + file.getAbsolutePath());
		return fileId;

	}


	private String extractExtensionOfFileName(String fileName) {
		String[] split = fileName.split("\\.");
		String fileExtension = "";
		if (split.length > 0) {
			fileExtension = '.' + split[split.length - 1];
		}
		return fileExtension;
	}

	
	private void closeStreamForFile(final String fileId) {

		//ask for the stream to close
		boolean needToCloseStream = rateLimiter.markRequestHasShallBeCancelled(fileId);
		
		if (needToCloseStream) {
			log.debug("waiting for the stream to be closed for "+fileId);

			//then wait for the stream to be closed
			Future<?> submit = streamCloserExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					while (rateLimiter.requestHasToBeCancelled(fileId)) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				}
			});
			try {
				//get the future
				submit.get(1, TimeUnit.MINUTES);
			}
			catch (Exception e) {
				//if we timeout delete anyway, just log
				log.debug("cannot confirm that the stream is closed for "+fileId);
			}
		}
		
	}
	
	public void clearFile(String fileId) {
		
		//ask for the stream to be closed
		log.debug("asking for deletion for file with id "+fileId);
		closeStreamForFile(fileId);

		//then delete
		staticStateManager.clearFile(fileId);
	}


	public void clearAll() {
		
		//close any pending stream before clearing the file
		for (String	 fileId : staticStateManager.getEntity().getFileStates().keySet()) {
			log.debug("asking for deletion for file with id "+fileId);
			closeStreamForFile(fileId);
		}
		
		//then clear everything
		staticStateManager.clear();
	}


	public Float getProgress(String fileId)
			throws FileNotFoundException {

		// get the file
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();
		StaticFileState fileState = model.getFileStates().get(fileId);
		if (fileState == null) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}
		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());

		// compare size of the file to the expected size
		Float progress = getProgress(file.length(), fileState.getStaticFileStateJson().getOriginalFileSizeInBytes()).floatValue();

		log.debug("progress for file " + fileId + ": " + progress);
		return progress;
	}


	Double getProgress(Long currentSize, Long expectedSize) {
		double percent = currentSize.doubleValue() / expectedSize.doubleValue() * 100d;
		if (percent == 100 && expectedSize - currentSize != 0) {
			percent = 99.99d;
		}
		return percent;
	}


	public Long getUploadStat(String fileId) {
		return rateLimiter.getUploadState(fileId);
	}


	public void setUploadRate(String fileId, Long rate) {

		// set the rate
		rateLimiter.assignRateToRequest(fileId, rate);

		// save it for the file with this file id
		StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();
		entity.getFileStates().get(fileId).getStaticFileStateJson().setRateInKiloBytes(rate);

		// save it to file system as the default
		entity.setDefaultRateInKiloBytes(rate);

		//persist changes
		staticStateManager.processEntityTreatment(entity);
	}

}
