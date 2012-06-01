package com.am.jlfu.notifier;


/**
 * Listener adapter of {@link JLFUListener}.
 * 
 * @author antoinem
 * 
 */
public class JLFUListenerAdapter
		implements JLFUListener {

	@Override
	public void onNewClient(String clientId) {

	}


	@Override
	public void onClientBack(String clientId) {

	}


	@Override
	public void onClientInactivity(String clientId, int inactivityDuration) {

	}


	@Override
	public void onFileUploadEnd(String clientId, String fileId) {

	}


	@Override
	public void onFileUploadPrepared(String clientId, String fileId) {

	}


	@Override
	public void onFileUploadCancelled(String clientId, String fileId) {

	}


	@Override
	public void onFileUploadPaused(String clientId, String fileId) {

	}


	@Override
	public void onFileUploadResumed(String clientId, String fileId) {

	}


}
