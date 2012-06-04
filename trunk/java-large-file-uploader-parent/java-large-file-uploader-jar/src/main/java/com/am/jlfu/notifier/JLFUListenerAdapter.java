package com.am.jlfu.notifier;


import java.util.UUID;



/**
 * Listener adapter of {@link JLFUListener}.
 * 
 * @author antoinem
 * 
 */
public class JLFUListenerAdapter
		implements JLFUListener {

	@Override
	public void onNewClient(UUID clientId) {

	}


	@Override
	public void onClientBack(UUID clientId) {

	}


	@Override
	public void onClientInactivity(UUID clientId, int inactivityDuration) {

	}


	@Override
	public void onFileUploadEnd(UUID clientId, UUID fileId) {

	}


	@Override
	public void onFileUploadPrepared(UUID clientId, UUID fileId) {

	}


	@Override
	public void onFileUploadCancelled(UUID clientId, UUID fileId) {

	}


	@Override
	public void onFileUploadPaused(UUID clientId, UUID fileId) {

	}


	@Override
	public void onFileUploadResumed(UUID clientId, UUID fileId) {

	}


}
