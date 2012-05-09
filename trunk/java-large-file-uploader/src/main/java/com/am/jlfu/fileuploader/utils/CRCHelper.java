package com.am.jlfu.fileuploader.utils;


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;



@Component
public class CRCHelper {

	private static final Logger log = LoggerFactory.getLogger(CRCHelper.class);



	public class CRCResult {

		private String crcAsString;
		private int streamLength;



		public String getCrcAsString() {
			return crcAsString;
		}


		public void setCrcAsString(String crcAsString) {
			this.crcAsString = crcAsString;
		}


		public int getStreamLength() {
			return streamLength;
		}


		public void setStreamLength(int streamLength) {
			this.streamLength = streamLength;
		}


	}



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
		log.debug("obtained crc for stream with length " + totalRead);

		CRCResult crcResult = new CRCResult();
		crcResult.setCrcAsString(Long.toHexString(crc32.getValue()));
		crcResult.setStreamLength(totalRead);
		return crcResult;

	}
}
