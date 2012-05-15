package com.am.jlfu.staticstate;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;



/**
 * Takes care of deleting the files.
 * 
 * @author antoinem
 * 
 */
@Component
public class FileDeleter
		implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(FileDeleter.class);

	/** The executor */
	private ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);



	@PostConstruct
	private void start() {
		executor.schedule(this, 10, TimeUnit.SECONDS);
	}



	/**
	 * List of the files to delete.
	 */
	private List<File> files = Lists.newArrayList();



	@Override
	public void run() {

		// extract all the files to an immutable list
		ImmutableList<File> copyOf;
		synchronized (files) {
			copyOf = ImmutableList.copyOf(files);
		}

		// and create a new list
		List<File> successfullyDeletedFiles = Lists.newArrayList();

		// delete them
		for (File file : copyOf) {
			if (delete(file)) {
				successfullyDeletedFiles.add(file);
				log.debug(file + " successfully deleted.");
			}
			else {
				log.debug(file + " not deleted, rescheduled for deletion.");
			}
		}

		// all the files have been processed
		// remove the deleted files from queue
		synchronized (files) {
			Iterables.removeAll(files, successfullyDeletedFiles);
		}

		// and reschedule
		start();
	}


	/**
	 * @param file
	 * @return true if the file has been deleted, false otherwise.
	 */
	private boolean delete(File file) {

		try {
			// if file exists
			if (file.exists()) {

				// if it is a file
				if (file.isFile()) {
					// delete it
					return file.delete();
				}
				// otherwise, if it is a directoy
				else if (file.isDirectory()) {
					FileUtils.deleteDirectory(file);
					return true;
				}
				// if its none of them, we cannot delete them so we assume its deleted.
				else {
					return true;
				}

			}
			// if does not exist, we can remove it from list
			else {
				return true;
			}
		}
		// if we have an exception
		catch (Exception e) {
			log.error(file + " deletion exception: " + e.getMessage());
			// the file has not been deleted
			return false;
		}

	}


	public void deleteFile(File... file) {
		deleteFiles(Arrays.asList(file));
	}


	public void deleteFiles(Collection<File> files) {
		synchronized (files) {
			this.files.addAll(files);
		}
	}

}
