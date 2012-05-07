package com.am.jlfu.fileuploader.limiter;





/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:01
 */
public interface TokenBucket {


	/**
	 * Try to acquire access.
	 * 
	 * @return
	 */
	boolean tryTake(String requestId);


	/**
	 * Release access lock.
	 * 
	 */
	void completed(String requestId);


}
