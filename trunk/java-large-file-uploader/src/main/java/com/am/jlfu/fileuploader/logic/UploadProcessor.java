package com.am.jlfu.fileuploader.logic;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.CRC32;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.InvalidCrcException;
import com.am.jlfu.fileuploader.json.FileStateJson;
import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.utils.Crc32;
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
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	StaticStateDirectoryManager staticStateDirectoryManager;

	@Autowired
	UploadLockMap lockMap;

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
		log.debug("File prepared for client "+staticStateIdentifierManager.getIdentifier() +" at path "+file.getAbsolutePath());
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


	public void process(InputStream inputStream, Long sliceFrom, String fileId, String receivedChecksum)
			throws IOException, InvalidCrcException {

		// one operation per client at a time
		String identifier = staticStateIdentifierManager.getIdentifier();


		// get the file
		StaticStatePersistedOnFileSystemEntity model = staticStateManager.getEntity();
		StaticFileState fileState = model.getFileStates().get(fileId);
		if (fileState == null) {
			throw new FileNotFoundException("File with id " + fileId + " for client " + identifier + " not found");
		}

		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());

		// if the file does not exist, there is an issue!
		if (!file.exists()) {
			throw new FileNotFoundException("File with id " + fileId + " for client " + identifier + " not found");
		}

		// Process file upload
		String calculatedChecksum = Long.toHexString(writeFromInputstreamToFile(inputStream, file));
		
		// TODO
		// compare the first chunk
		
		//compare the checksum of the chunks
		if (!calculatedChecksum.equals(receivedChecksum)) {
			throw new InvalidCrcException(calculatedChecksum, receivedChecksum);
		}

		// persist file
		if (log.isDebugEnabled()) {
			log.debug("Processed part " + sliceFrom + " for file " + fileId + " for client " +
					identifier + " into temp file");
		}


	}


	public void clearFile(String fileId) {
		staticStateManager.clearFile(fileId);

	}


	public void clearAll() {
		staticStateManager.clear();
	}



	int BUFFER_SIZE = 1024 * 2;



	private long writeFromInputstreamToFile(InputStream in, File file)
			throws IOException {
		log.debug("-begining writing stream to " + file.getName() + "-");


		byte[] buffer = new byte[BUFFER_SIZE];

		BufferedInputStream bin = new BufferedInputStream(in, BUFFER_SIZE);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, true), BUFFER_SIZE);
		CRC32 crc32 = new CRC32();
		int n = 0;
		try {
			while ((n = bin.read(buffer, 0, BUFFER_SIZE)) != -1) {
				out.write(buffer, 0, n);
				crc32.update(buffer, 0, n);
			}
			out.flush();
		}
		finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
		}
		log.debug("-finished writing stream to " + file.getName() + "-");

		return crc32.getValue();
	}


	public Float getProgress(String fileId) throws FileNotFoundException {

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

}
