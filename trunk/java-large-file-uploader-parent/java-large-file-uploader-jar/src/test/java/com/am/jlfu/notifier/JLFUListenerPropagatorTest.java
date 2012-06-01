package com.am.jlfu.notifier;


import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class JLFUListenerPropagatorTest {

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;

	private int testCounter;

	private JLFUListenerAdapter listener;



	@Before
	public void before() {
		testCounter = 0;
		listener = new JLFUListenerAdapter() {

			@Override
			public void onNewClient(String clientId) {
				testCounter++;
			}
		};

	}


	@Test
	public void test() {

		// add two listener
		jlfuListenerPropagator.registerListener(listener);
		jlfuListenerPropagator.registerListener(listener);

		// trigger event
		jlfuListenerPropagator.getPropagator().onNewClient("client");

		// assert
		Assert.assertThat(testCounter, CoreMatchers.is(2));

		// unregister one listener
		jlfuListenerPropagator.unregisterListener(listener);

		// trigger event
		jlfuListenerPropagator.getPropagator().onNewClient("client");

		// assert
		Assert.assertThat(testCounter, CoreMatchers.is(3));

	}
}
