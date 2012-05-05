package com.am.jlfu.fileuploader.utils;


import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.utils.UploadLockMap;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadLockMapTest {

	private int nThreads = 3;

	@Autowired
	UploadLockMap uploadLockMap;



	@Test
	public void testConcurrencySuccess()
			throws InterruptedException, ExecutionException {
		Collection<Object> testConcurrency = testConcurrency(true);
		Assert.assertEquals(nThreads, testConcurrency.size());
	}


	@Test
	public void testConcurrencyFail()
			throws InterruptedException, ExecutionException {
		Collection<Object> testConcurrency = testConcurrency(false);
		Assert.assertTrue(testConcurrency.size() < nThreads);
	}


	public Collection<Object> testConcurrency(final boolean success)
			throws InterruptedException, ExecutionException {
		ListeningExecutorService newFixedThreadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nThreads));
		Collection<Callable<Object>> runnables = Lists.newArrayList();
		for (int i = 0; i < nThreads; i++) {
			runnables.add(new Callable<Object>() {

				@Override
				public Object call()
						throws Exception {
					if (success) {
						synchroMethod();
					}
					else {
						method();
					}
					return Boolean.TRUE;
				}
			});
		}
		List invokeAll = newFixedThreadPool.invokeAll(runnables);
		newFixedThreadPool.shutdown();
		newFixedThreadPool.awaitTermination(100, TimeUnit.MILLISECONDS);
		ListenableFuture<List<Object>> successfulAsList =
				Futures.successfulAsList(invokeAll);
		return Collections2.filter(successfulAsList.get(), Predicates.notNull());
	}


	private void synchroMethod() {
		synchronized (uploadLockMap.getUnchecked("lala")) {
			method();
		}
	}



	private Object object = "lala";



	private void method() {
		for (int i = 0; i < 10; i++) {
			object.toString();
			object = null;
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				throw new RuntimeException();
			}
			object = "lala";
		}
	}


}
