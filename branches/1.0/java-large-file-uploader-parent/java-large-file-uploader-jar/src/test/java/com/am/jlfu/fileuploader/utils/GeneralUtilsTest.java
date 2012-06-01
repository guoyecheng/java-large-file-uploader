package com.am.jlfu.fileuploader.utils;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class GeneralUtilsTest {

	private static final Logger log = LoggerFactory.getLogger(GeneralUtilsTest.class);

	@Autowired
	GeneralUtils generalUtils;

	private volatile int i = 0;



	class FailingConditionProvider extends ConditionProvider {

		@Override
		public boolean condition() {
			return false;
		}


		@Override
		public void onFail() {
			log.debug(i++ + " failing failed: ok ");
		}


		@Override
		public void onSuccess() {
			Assert.fail();
		}
	}
	class SuccessConditionProvider extends ConditionProvider {

		@Override
		public boolean condition() {
			return true;
		}


		@Override
		public void onSuccess() {
			log.debug(i++ + " success succeeded: ok ");
		}


		@Override
		public void onFail() {
			Assert.fail();
		}
	}



	@Test
	public void testSuccess() {
		List<ConditionProvider> conditionsProviders = Lists.newArrayList();
		conditionsProviders.add(new SuccessConditionProvider());
		Assert.assertTrue(generalUtils.waitForConditionToCompleteRunnable(5, conditionsProviders));
	}


	@Test
	public void test() {

		List<ConditionProvider> conditionsProviders = Lists.newArrayList();
		for (int i = 0; i < 100; i++) {
			conditionsProviders.add(i % 2 == 0 ? new SuccessConditionProvider() : new FailingConditionProvider());
		}
		Assert.assertFalse(generalUtils.waitForConditionToCompleteRunnable(10, conditionsProviders));
	}
}
