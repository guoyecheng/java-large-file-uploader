package com.am.jlfu.staticstate;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.JLFUListenerPropagator;
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

	@Autowired
	FileDeleter fileDeleter;

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;

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



	LoadingCache<UUID, T> cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build(new CacheLoader<UUID, T>() {

		public T load(UUID uuid)
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
	private T createOrRestore(UUID uuid)
			throws IOException {

		// restore cache from file:
		File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();

		// if that file is scheduled for deletion, we do not restore it
		if (fileDeleter.deletionQueueContains(uuidFileParent)) {
			log.debug("trying to restore from state a file that is scheduled for deletion");
			// invalidate identifier
			staticStateIdentifierManager.clearIdentifier();
			// get a new one
			// and recreate file
			uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();
		}

		File uuidFile = new File(uuidFileParent, FILENAME);
		T entity = null;

		if (uuidFile.exists()) {
			log.debug("No value in the cache for uuid " + uuid + ". Filling cache from file.");
			try {
				entity = read(uuidFile);
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
	 * Retrieves the entity from cache using a client identifier
	 * 
	 * @param clientIdentifier
	 * @return
	 */
	public T getEntityIfPresentWithIdentifier(UUID clientIdentifier) {
		return cache.getIfPresent(clientIdentifier);
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
	 * Retrieves the entity from cache or null if this entity is not present.
	 * 
	 * @return
	 */
	public StaticStatePersistedOnFileSystemEntity getEntityIfPresent() {
		return getEntityIfPresentWithIdentifier(staticStateIdentifierManager.getIdentifier());
	}


	/**
	 * Persist modifications to file and cache.
	 * 
	 * @param entity
	 * @return
	 * @throws ExecutionException
	 */
	public void processEntityTreatment(T entity) {
		UUID uuid = staticStateIdentifierManager.getIdentifier();
		log.debug("writing state for " + uuid);
		cache.put(uuid, entity);
		write(entity, new File(staticStateDirectoryManager.getUUIDFileParent(), FILENAME));
	}


	/**
	 * Persists modifications onto filesystem only.
	 * 
	 * @param entity
	 */
	public void writeEntity(UUID uuid, T entity) {
		write(entity, new File(staticStateDirectoryManager.getUUIDFileParent(uuid), FILENAME));

	}


	/**
	 * Clear everything including cache, session, files.
	 * 
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void clear()
	{
		log.debug("Clearing everything including cache, session, files.");

		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();

		// schedule file for deletion
		fileDeleter.deleteFile(uuidFileParent);

		// remove entity from cache
		cache.invalidate(staticStateIdentifierManager.getIdentifier());

		// remove cookie and session
		staticStateIdentifierManager.clearIdentifier();

	}


	public void clearFile(final UUID fileId)
	{
		log.debug("Clearing pending uploaded file and all attributes linked to it.");

		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent();


		// remove the uploaded file for this particular id
		fileDeleter.deleteFile(uuidFileParent.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith(fileId.toString());
			}
		}));

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


	T read(File f) {
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
	 * @param clientId
	 * @param fileId
	 * @return true if the file is complete
	 */
	public void setCrcBytesValidated(final UUID clientId, UUID fileId, final long validated) {

		final T entity = cache.getIfPresent(clientId);
		if (entity == null) {
			return;
		}
		final StaticFileState staticFileState = entity.getFileStates().get(fileId);
		Long crcredBytes = staticFileState.getStaticFileStateJson().getCrcedBytes();
		staticFileState.getStaticFileStateJson().setCrcedBytes(
				crcredBytes + validated);

		log.debug(validated + " more bytes have been validated appended to the already " + crcredBytes + " bytes validated for file " + fileId +
				" for client id " + clientId);

		// manage the end of file
		if (staticFileState.getStaticFileStateJson().getCrcedBytes() == staticFileState.getStaticFileStateJson().getOriginalFileSizeInBytes()) {
			jlfuListenerPropagator.getPropagator().onFileUploadEnd(clientId, fileId);
		}

		// TODO remove when stable or make a proper end of upload verification with crc of last
		// chunk?
		if (staticFileState.getStaticFileStateJson().getCrcedBytes() > staticFileState.getStaticFileStateJson().getOriginalFileSizeInBytes()) {
			log.error("###############################################################################");
			log.error("###############################################################################");
			log.error("# " + staticFileState.getStaticFileStateJson().getCrcedBytes() + " crced bytes are more than it should be: " +
					staticFileState.getStaticFileStateJson().getOriginalFileSizeInBytes() + " #");
			log.error("###############################################################################");
			log.error("###############################################################################");
		}

		fileStateUpdaterExecutor.submit(new Runnable() {

			@Override
			public void run() {
				// write this later on.
				writeEntity(clientId, entity);
			}
		});

	}

}
