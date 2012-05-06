package com.am.jlfu.fileuploader.limiter;

import javax.servlet.ServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:01
 */
public interface TokenBucket {

	int SIZE_OF_THE_BUFFER_FOR_A_TOKEN_PROCESSING_IN_BYTES = 1024 * 10;
	
	/** Identifier of the request number injected in the request. */
	public static final String REQUEST_NO = "REQUEST_NO";

	/**
	 * Try to acquire access.
	 * @param req
	 * @return
	 */
	boolean tryTake(ServletRequest req);
	
	/**
	 * Release access lock.
	 * @param req
	 */
	void completed(ServletRequest req);
}