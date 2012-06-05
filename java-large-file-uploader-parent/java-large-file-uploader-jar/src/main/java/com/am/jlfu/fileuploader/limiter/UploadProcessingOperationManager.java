package com.am.jlfu.fileuploader.limiter;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;



@Component
public class UploadProcessingOperationManager {

	// ////////////
	// operation//
	// ////////////

	/** Operation for clients and requests. */
	final ConcurrentMap<UUID, UploadProcessingOperation> clientsAndRequestsProcessingOperation = Maps.newConcurrentMap();

	/** Operation for master. */
	final UploadProcessingOperation masterProcessingOperation = new UploadProcessingOperation();

	// ////////////

	/** Maps a client to its current requests */
	final ConcurrentMap<UUID, Set<UUID>> clientToRequestsMapping = Maps.newConcurrentMap();



	public Map<UUID, UploadProcessingOperation> getClientsAndRequestsProcessingOperation() {
		return clientsAndRequestsProcessingOperation;
	}


	public Map<UUID, Set<UUID>> getClientToRequestsMapping() {
		return clientToRequestsMapping;
	}


	public void startOperation(UUID clientId, UUID fileId) {

		// create the request one
		// XXX are we sure that there is only one there?
		clientsAndRequestsProcessingOperation.put(fileId, new UploadProcessingOperation());

		// get or create the client one
		clientsAndRequestsProcessingOperation.putIfAbsent(clientId, new UploadProcessingOperation());

		// mapping
		Set<UUID> set = clientToRequestsMapping.get(clientId);
		if (set == null) {
			set = new HashSet<UUID>();
			clientToRequestsMapping.put(clientId, set);
		}
		set.add(fileId);

	}


	public boolean stopOperation(UUID clientId, UUID fileId) {

		// remove from map
		clientsAndRequestsProcessingOperation.remove(fileId);

		// remove mapping
		clientToRequestsMapping.get(clientId).remove(fileId);

		// if client is empty, remove client
		final boolean noreMoreUploadsForThisClient = clientToRequestsMapping.get(clientId).isEmpty();
		if (noreMoreUploadsForThisClient) {
			clientToRequestsMapping.remove(clientId);
			clientsAndRequestsProcessingOperation.remove(clientId);
		}

		return noreMoreUploadsForThisClient;
	}


	public UploadProcessingOperation getClientProcessingOperation(UUID clientId) {
		return clientsAndRequestsProcessingOperation.get(clientId);
	}


	public UploadProcessingOperation getFileProcessingOperation(UUID fileId) {
		return clientsAndRequestsProcessingOperation.get(fileId);
	}


	public UploadProcessingOperation getMasterProcessingOperation() {
		return masterProcessingOperation;
	}
}
