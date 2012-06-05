package com.am.jlfu.fileuploader.limiter;


import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.notifier.JLFUListenerAdapter;
import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.StaticStateIdentifierManager;
import com.am.jlfu.staticstate.StaticStateManager;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.google.common.cache.RemovalCause;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RateLimiterConfigurationManagerTest {

	@Autowired
	RateLimiterConfigurationManager rateLimiterConfigurationManager;

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	private boolean assertt = false;



	@Before
	public void init() {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());

	}


	@Test
	public void testEvictionNotificationTrue()
			throws InterruptedException {
		testAssert(true);
	}


	@Test
	public void testEvictionNotificationFalse()
			throws InterruptedException {
		testAssert(false);
	}


	private void testAssert(boolean lala) {
		jlfuListenerPropagator.registerListener(new JLFUListenerAdapter() {

			@Override
			public void onClientInactivity(UUID clientId, int inactivityDuration) {
				assertt = true;
			}
		});

		// emulate a pending upload
		final StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();
		final UUID identifier = staticStateIdentifierManager.getIdentifier();
		rateLimiterConfigurationManager.requestConfigMap
				.put(identifier, new RequestUploadProcessingConfiguration());
		rateLimiterConfigurationManager.requestConfigMap.getUnchecked(identifier);
		final StaticFileState value = new StaticFileState();
		entity.getFileStates().put(identifier, value);
		final FileStateJsonBase staticFileStateJson = new FileStateJsonBase();
		value.setStaticFileStateJson(staticFileStateJson);
		staticFileStateJson.setCrcedBytes(100l);

		if (lala) {
			staticFileStateJson.setOriginalFileSizeInBytes(10000l);
		}
		else {
			staticFileStateJson.setOriginalFileSizeInBytes(100l);
		}

		rateLimiterConfigurationManager.remove(RemovalCause.EXPIRED, identifier);

		if (lala) {
			Assert.assertThat(assertt, CoreMatchers.is(true));
		}
		else {
			Assert.assertThat(assertt, CoreMatchers.is(false));
		}
	}

}
