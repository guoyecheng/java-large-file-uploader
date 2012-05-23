package com.am.jlfu.fileuploader.utils;


import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;



@Component
public class UploadLockMap extends ForwardingLoadingCache<String, Object> {

	private final LoadingCache<String, Object> delegate;



	/**
	 * Constructor.
	 */
	UploadLockMap() {
		this.delegate =
				CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS)
						.build(new CacheLoader<String, Object>() {

							@Override
							public Object load(String key)
									throws Exception {
								return new Object();
							}

						});
	}


	@Override
	protected LoadingCache<String, Object> delegate() {
		return delegate;
	}
}
