package com.am.jlfu.staticstate;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.notifier.JLFUListenerPropagator;



@Component
public class StaticStateIdentifierManager {

	public static final String cookieIdentifier = "jlufStaticStateCookieName";

	@Autowired
	RequestComponentContainer requestComponentProvider;

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;



	static Cookie getCookie(Cookie[] cookies, String id) {
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(id)) {
					// found it
					if (cookie.getMaxAge() != 0) {
						return cookie;
					}
				}
			}
		}
		return null;
	}


	void setCookie(String uuid) {
		Cookie cookie = new Cookie(cookieIdentifier, uuid);
		cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(31));
		requestComponentProvider.getResponse().addCookie(cookie);
	}


	String getUuid() {
		final String uuid = UUID.randomUUID().toString();
		jlfuListenerPropagator.getPropagator().onNewClient(uuid);
		return uuid;
	}


	/**
	 * Retrieves the identifier from a cookie or create a new one if not found.
	 * 
	 * @return
	 */
	public String getIdentifier() {

		// get from session
		String uuid = (String) requestComponentProvider.getSession().getAttribute(cookieIdentifier);

		// if nothing in session
		if (uuid == null) {

			// check in cookie
			Cookie cookie = getCookie(requestComponentProvider.getRequest().getCookies(), cookieIdentifier);
			if (cookie != null && cookie.getValue() != null) {
				// set in session
				requestComponentProvider.getSession().setAttribute(cookieIdentifier, cookie.getValue());
				jlfuListenerPropagator.getPropagator().onClientBack(uuid);
				return cookie.getValue();
			}

			// if not in session nor cookie, create one
			// create uuid
			uuid = getUuid();

			// then set in session
			requestComponentProvider.getSession().setAttribute(cookieIdentifier, uuid);

			// and cookie
			setCookie(uuid);

		}
		return uuid;
	}


	/**
	 * Removes the identifier from session and cookie
	 */
	public void clearIdentifier() {
		// clear session
		requestComponentProvider.getSession().removeAttribute(cookieIdentifier);

		// remove cookie
		Cookie cookie = getCookie(requestComponentProvider.getRequest().getCookies(), cookieIdentifier);
		if (cookie != null) {
			cookie.setMaxAge(0);
			requestComponentProvider.getResponse().addCookie(cookie);
		}
	}


}
