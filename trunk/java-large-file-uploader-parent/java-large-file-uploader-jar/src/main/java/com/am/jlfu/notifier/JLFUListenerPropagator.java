package com.am.jlfu.notifier;


import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.utils.GenericPropagator;



/**
 * Propagates all events fired on {@link #proxiedListener} to the {@link #listeners}.
 * 
 * @author antoinem
 * 
 */
@Component
public class JLFUListenerPropagator extends GenericPropagator<JLFUListener> {

	@Override
	protected Class<JLFUListener> getProxiedClass() {
		return JLFUListener.class;
	}

}
