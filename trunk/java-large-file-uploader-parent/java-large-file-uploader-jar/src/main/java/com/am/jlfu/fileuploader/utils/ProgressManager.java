package com.am.jlfu.fileuploader.utils;


import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.JLFUListener;
import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.StaticStateManagerService;
import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.google.common.collect.Maps;



/**
 * Component responsible for advertising the write progress of a specific file to
 * {@link JLFUListener}s.<br>
 * Every second, it calculates the progress of all the files being uploaded.
 * 
 * @author antoinem
 * 
 */
@Component
public class ProgressManager {
	private static final Logger log = LoggerFactory.getLogger(ProgressManager.class);

	@Autowired
	private JLFUListenerPropagator jlfuListenerPropagator;

	@Autowired
	private ClientToFilesMap clientToFilesMap;
	
	@Autowired
	private StaticStateManagerService<StaticStatePersistedOnFileSystemEntity> staticStateManagerService;

	/** Internal map. */
	Map<UUID, FileProgressStatus> fileToProgressInfo = Maps.newHashMap();
	
	/** Simple advertiser. */
	ProgressManagerAdvertiser progressManagerAdvertiser = new ProgressManagerAdvertiser();
	
	@Scheduled(fixedRate = 1000)
	public void calculateProgress() {

		synchronized (fileToProgressInfo) {
		
			//for all clients
			for (Entry<UUID, Set<UUID>> entry : clientToFilesMap.entrySet()) {
				
				//for all pending upload
				for (UUID fileId : entry.getValue()) {
				
					try {
						
						//calculate its progress
						FileProgressStatus newProgress = staticStateManagerService.getProgress(entry.getKey(), fileId);

						//if progress has successfully been computed
						if (newProgress != null) {
							
							//get from map
							FileProgressStatus progressInMap = fileToProgressInfo.get(fileId);
							
							//if not present in map
							//or if present in map but different from previous one
							if (progressInMap == null || !Float.valueOf(progressInMap.getPercentageCompleted()).equals(newProgress.getPercentageCompleted())) {
								
								//add to map
								fileToProgressInfo.put(fileId, newProgress);
								
								// and avertise
								progressManagerAdvertiser.advertise(entry.getKey(), fileId, newProgress);
								
							}
						}
						
					}
					catch (FileNotFoundException e) {
						log.debug("cannot retrieve progress for "+fileId);
					}
					
				}
			}

		}
	}
	
	class ProgressManagerAdvertiser {
		
		void advertise(final UUID clientId, final UUID fileId, final FileProgressStatus newProgress) {
			
			new Runnable() {
				
				@Override
				public void run() {
					jlfuListenerPropagator.getPropagator().OnFileUploadProgress(clientId, fileId, newProgress);
				}
			};
			
		}
	}

	/**
	 * Returns a calculated progress of a pending file upload.<br>
	 * returns 0 if not yet calculated.
	 * @param fileId
	 * @return
	 */
	public FileProgressStatus getProgress(UUID fileId) {
		synchronized (fileToProgressInfo) {
			return fileToProgressInfo.get(fileId);
		}
	}


	
}
