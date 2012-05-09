package com.am.jlfu.staticstate;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.thoughtworks.xstream.XStream;



/**
 * Has to be initialized with the {@link #init(Class)} method first.
 * 
 * @author antoinem
 * 
 * @param <T>
 */
@Component
public class StaticStateManager<T extends StaticStatePersistedOnFileSystemEntity> {

	private static final Logger log = LoggerFactory.getLogger(StaticStateManager.class);
	private static final String FILENAME = "StaticState.xml";
	public static final int DELETION_DELAY = 100;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	StaticStateDirectoryManager staticStateDirectoryManager;

	/**
	 * Used to bypass generic type erasure.<br>
	 * Has to be manually specified with the {@link #init(Class)} method.
	 */
	Class<T> entityType;


	/** The executor that could write stuff asynchronously into the static state */
	private ThreadPoolExecutor fileStateUpdaterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);



	private Class<T> getEntityType() {
		// if not defined, try to init with default
		if (entityType == null) {
			entityType = (Class<T>) StaticStatePersistedOnFileSystemEntity.class;
		}
		return entityType;
	}



	LoadingCache<String, T> cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build(new CacheLoader<String, T>() {

		public T load(String uuid)
				throws Exception {

			return createOrRestore(uuid);

		}
	});



	/**
	 * Creates or restores a new file.
	 * 
	 * @param uuid
	 * @return
	 * @throws IOException
	 */
	private T createOrRestore(String uuid)
			throws IOException {
		// restore cache from file:
		File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();
		File uuidFile = new File(uuidFileParent, FILENAME);
		T entity = null;

		if (uuidFile.exists()) {
			log.debug("No value in the cache for uuid " + uuid + ". Filling cache from file.");
			try {
				entity = read(uuid, uuidFile);
			}
			catch (Exception e) {
				log.error("Cache cannot be restored from " + uuidFile.getAbsolutePath() + "." +
						"The file might be empty or the model has changed since last time: " + e.getMessage(), e);
			}
		}
		else {
			log.debug("No value in the cache for uuid " + uuid + " and no value in the file. Creating a new one.");

			// create the file
			try {
				uuidFile.createNewFile();
			}
			catch (IOException e) {
				log.error("cannot create model file: " + e.getMessage(), e);
				throw e;
			}

			// and persist an entity
			try {
				entity = getEntityType().newInstance();
			}
			catch (InstantiationException e) {
				throw new RuntimeException(e);
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			write(entity, uuidFile);

		}

		// then return entity
		return entity;
	}


	/**
	 * Retrieves the entity from cookie or cache if it exists or create one if it does not exists.
	 * 
	 * @return
	 * @throws ExecutionException
	 */
	public T getEntity() {
		return cache.getUnchecked(staticStateIdentifierManager.getIdentifier());
	}


	/**
	 * Persist modifications to file.
	 * 
	 * @param uuid
	 * @param entity
	 * @return
	 * @throws ExecutionException
	 */
	public void processEntityTreatment(T entity) {
		String uuid = staticStateIdentifierManager.getIdentifier();
		cache.put(uuid, entity);
		write(entity, new File(staticStateDirectoryManager.getUUIDFileParent(), FILENAME));
	}


	/**
	 * Clear everything including cache, session, files.
	 * 
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void clear()
			throws InterruptedException, ExecutionException, TimeoutException {
		log.debug("Clearing everything including cache, session, files.");

		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();


		// remove files
		try {
			FileUtils.deleteDirectory(uuidFileParent);
		}
		catch (IOException e) {
			log.error(
					"Cannot delete file located at " +
							uuidFileParent +
							". Manual intervention required to immediately free space. Note that this directory should be automatically deleted by the quarts cleaner job. Cause: " +
							e.getMessage(), e);
		}


		// remove entity from cache
		cache.invalidate(staticStateIdentifierManager.getIdentifier());

		// remove cookie and session
		staticStateIdentifierManager.clearIdentifier();

	}


	public void clearFile(final String fileId)
			throws InterruptedException, ExecutionException, TimeoutException {
		log.debug("Clearing pending uploaded file and all attributes linked to it.");

		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();


		// remove the uploaded file for this particular id
		File[] listFiles = uuidFileParent.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith(fileId);
			}
		});
		if (listFiles.length > 0) {
			File file = listFiles[0];
			if (file.exists()) {
				file.delete();
			}
		}


		// remove the file information in entity
		T entity = getEntity();
		entity.getFileStates().remove(fileId);

		// and save
		processEntityTreatment(entity);
	}


	private void write(T modelFromContext, File modelFile) {
		XStream xStream = new XStream();
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(modelFile);
			xStream.toXML(modelFromContext, fs);
		}
		catch (FileNotFoundException e) {
			log.error("cannot write to model file for " + modelFromContext.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		finally {
			IOUtils.closeQuietly(fs);
		}
	}


	T read(String uuid, File f) {
		XStream xStream = new XStream();
		FileInputStream fs = null;
		T fromXML = null;
		try {
			fs = new FileInputStream(f);
			fromXML = (T) xStream.fromXML(fs);
		}
		catch (FileNotFoundException e) {
			log.error("cannot read model file: " + e.getMessage(), e);
		}
		finally {
			IOUtils.closeQuietly(fs);
		}
		return fromXML;
	}


	/**
	 * Initializes the bean with the class of the entity. Shall be called once. Calling it more than
	 * once has no effect.
	 * 
	 * @param clazz
	 */
	public void init(Class<T> clazz) {
		entityType = clazz;
	}


	/**
	 * Writes in the file that the last slice has been successfully uploaded.
	 * 
	 * @param fileId
	 */
	public void setCrcBytesValidated(final String fileId, final long validated) {
		fileStateUpdaterExecutor.submit(new Runnable() {

			@Override
			public void run() {
				log.debug("writing bytes validated to file");
				final StaticStatePersistedOnFileSystemEntity entity = getEntity();
				final StaticFileState staticFileState = entity.getFileStates().get(fileId);
				staticFileState.getStaticFileStateJson().setCrcedBytes(
						staticFileState.getStaticFileStateJson().getCrcedBytes() + validated);
				processEntityTreatment((T) entity);
			}
		});
	}


}
