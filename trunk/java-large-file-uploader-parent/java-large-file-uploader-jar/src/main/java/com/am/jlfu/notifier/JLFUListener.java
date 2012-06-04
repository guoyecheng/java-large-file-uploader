package com.am.jlfu.notifier;


import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager;
import com.am.jlfu.identifier.impl.DefaultIdentifierProvider;



/**
 * Listener to be able to monitor the JLFU API on the java side.
 * Use {@link JLFUListenerPropagator} to register a listener.
 * 
 * @author antoinem
 * 
 */
public interface JLFUListener {

	/**
	 * Fired when a new client has been attributed a new id.<br>
	 * <i>Note that this event is sent by the {@link DefaultIdentifierProvider}.</i>
	 * 
	 * @param clientId
	 */
	void onNewClient(String clientId);


	/**
	 * Fired when a client is identified with its cookie and the corresponding state is restored.
	 * <i>Note that this event is sent by the {@link DefaultIdentifierProvider}.</i>
	 * 
	 * @param clientId
	 */
	void onClientBack(String clientId);


	/**
	 * Fired when the uploads of a client have been inactive for duration specified.<br>
	 * Default to {@link RateLimiterConfigurationManager#CLIENT_EVICTION_TIME_IN_MINUTES}
	 * 
	 * @param clientId
	 * @param inactivityDuration
	 */
	void onClientInactivity(String clientId, int inactivityDuration);


	/**
	 * Fired when the upload of the file specified by the fileId is finished for the client
	 * specified by the clientId.
	 * 
	 * @param clientId
	 * @param fileId
	 */
	void onFileUploadEnd(String clientId, String fileId);


	/**
	 * Fired when the upload of the file specified by the fileId has been prepared for the client
	 * specified by the clientId.
	 * 
	 * @param clientId
	 * @param fileId
	 */
	void onFileUploadPrepared(String clientId, String fileId);


	/**
	 * Fired when the upload of the file specified by the fileId has been cancelled for the client
	 * specified by the clientId.
	 * 
	 * @param clientId
	 * @param fileId
	 */
	void onFileUploadCancelled(String clientId, String fileId);


	/**
	 * Fired when the upload of the file specified by the fileId has been paused for the client
	 * specified by the clientId.
	 * 
	 * @param clientId
	 * @param fileId
	 */
	void onFileUploadPaused(String clientId, String fileId);


	/**
	 * Fired when the upload of the file specified by the fileId has been resumed for the client
	 * specified by the clientId.
	 * 
	 * @param clientId
	 * @param fileId
	 */
	void onFileUploadResumed(String clientId, String fileId);


}
