package com.am.jlfu.fileuploader.utils;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;



/**
 * Provides a bunch of utility
 * 
 * @author antoinem
 * 
 */
@Component
public class GeneralUtils {

	private ExecutorService conditionExecutorService = Executors.newFixedThreadPool(20);



	public boolean waitForConditionToCompleteRunnable(final int timeToWaitInMillis, ConditionProvider... conditionProvider) {
		return waitForConditionToCompleteRunnable(timeToWaitInMillis, Arrays.asList(conditionProvider));
	}


	public boolean waitForConditionToCompleteRunnable(final int timeToWaitInMillis, List<ConditionProvider> conditionProviders) {
		boolean success = true;
		Map<Future<Boolean>, ConditionProvider> futures = Maps.newLinkedHashMap();

		// submit
		for (final ConditionProvider conditionProvider : conditionProviders) {
			futures.put(conditionExecutorService.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() {
					DateTime start = new DateTime();
					while (!conditionProvider.condition()) {
						if (new DateTime().isAfter(start.plusMillis(timeToWaitInMillis))) {
							return false;
						}
						try {
							Thread.sleep(5);
						}
						catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					return true;
				}
			}), conditionProvider);
		}

		// check
		for (Entry<Future<Boolean>, ConditionProvider> entry : futures.entrySet()) {
			try {
				if (entry.getKey().get()) {
					entry.getValue().onSuccess();
				}
				else {
					entry.getValue().onFail();
					success = false;
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException();
			}
			catch (ExecutionException e) {
				throw new RuntimeException();
			}
		}
		return success;
	}
}
