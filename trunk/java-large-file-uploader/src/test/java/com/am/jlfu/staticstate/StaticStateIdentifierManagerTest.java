package com.am.jlfu.staticstate;


import javax.servlet.http.Cookie;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class StaticStateIdentifierManagerTest {

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
	MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();



	@Before
	public void init() {

		// populate request component container
		requestComponentContainer.populate(mockHttpServletRequest, mockHttpServletResponse);

		// clean cookie
		mockHttpServletRequest.clearAttributes();
		mockHttpServletRequest.setCookies(new Cookie[] {});
		mockHttpServletResponse.reset();
	}


	@Test
	public void testNoIdInCookieOrSession() {

		// assert session is empty
		Assert.assertNull(mockHttpServletRequest.getSession().getAttribute(StaticStateIdentifierManager.cookieIdentifier));

		// assert cookie is empty
		Assert.assertNull(StaticStateIdentifierManager.getCookie(mockHttpServletRequest.getCookies(), StaticStateIdentifierManager.cookieIdentifier));

		// get id
		String identifier = staticStateIdentifierManager.getIdentifier();

		// copy cookies from response to request
		mockHttpServletRequest.setCookies(mockHttpServletResponse.getCookies());

		Assert.assertNotNull(identifier);

		// assert cookie filled
		Assert.assertEquals(identifier,
				StaticStateIdentifierManager.getCookie(mockHttpServletRequest.getCookies(), StaticStateIdentifierManager.cookieIdentifier).getValue());

		// assert session filled
		Assert.assertEquals(identifier, mockHttpServletRequest.getSession().getAttribute(StaticStateIdentifierManager.cookieIdentifier));

		// then clear identifier
		staticStateIdentifierManager.clearIdentifier();

		// copy cookies from response to request
		mockHttpServletRequest.setCookies(mockHttpServletResponse.getCookies());

		// assert session is empty
		Assert.assertNull(mockHttpServletRequest.getSession().getAttribute(staticStateIdentifierManager.cookieIdentifier));

		// assert cookie is either empty or maxage below 0
		Assert.assertNull(StaticStateIdentifierManager.getCookie(mockHttpServletRequest.getCookies(), StaticStateIdentifierManager.cookieIdentifier));
	}


	@Test
	public void testNoIdInSession() {

		// assert session is empty
		Assert.assertNull(mockHttpServletRequest.getSession().getAttribute(staticStateIdentifierManager.cookieIdentifier));

		// assert cookie is empty
		Assert.assertNull(StaticStateIdentifierManager.getCookie(mockHttpServletRequest.getCookies(), StaticStateIdentifierManager.cookieIdentifier));

		// set cookie
		String identifierOriginal = staticStateIdentifierManager.getUuid();
		staticStateIdentifierManager.setCookie(identifierOriginal);

		// copy cookies from response to request
		mockHttpServletRequest.setCookies(mockHttpServletResponse.getCookies());

		// assert cookie filled
		Assert.assertEquals(identifierOriginal,
				StaticStateIdentifierManager.getCookie(mockHttpServletRequest.getCookies(), StaticStateIdentifierManager.cookieIdentifier).getValue());

		// get id
		String identifier = staticStateIdentifierManager.getIdentifier();
		Assert.assertEquals(identifierOriginal, identifier);

		// assert session is filled with id
		Assert.assertEquals(identifier, mockHttpServletRequest.getSession().getAttribute(StaticStateIdentifierManager.cookieIdentifier));

	}

}
