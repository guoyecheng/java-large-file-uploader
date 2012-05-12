package com.am.jlfu.fileuploader.utils;


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.json.CRCResult;
import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;



@Component
public class CRCHelper {

	private static final Logger log = LoggerFactory.getLogger(CRCHelper.class);


	public CRCResult getBufferedCrc(InputStream inputStream)
			throws IOException {

		byte[] b = new byte[UploadServletAsyncProcessor.SIZE_OF_THE_BUFFER_IN_BYTES];
		int read;
		int totalRead = 0;
		CRC32 crc32 = new CRC32();
		while ((read = inputStream.read(b)) != -1) {
			crc32.update(b, 0, read);
			totalRead += read;
		}
		inputStream.close();

		CRCResult crcResult = new CRCResult();
		crcResult.setCrcAsString(Long.toHexString(crc32.getValue()));
		crcResult.setTotalRead(totalRead);

		log.debug("obtained crc for stream with length " + totalRead + " : " + crcResult.getCrcAsString());

		return crcResult;

	}
}
