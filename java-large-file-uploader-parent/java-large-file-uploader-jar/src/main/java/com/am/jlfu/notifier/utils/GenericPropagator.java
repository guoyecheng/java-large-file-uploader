package com.am.jlfu.notifier.utils;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.am.jlfu.notifier.JLFUListener;
import com.google.common.collect.Lists;



/**
 * Propagates the methods called on {@link #proxiedElement} to all objects in {@link #propagateTo}.<br>
 * {@link #getProxiedClass()} has to be overridden by any subclass.
 * 
 * @param <T>
 * @author antoinem
 */
public abstract class GenericPropagator<T> {

	private static final Logger log = LoggerFactory.getLogger(GenericPropagator.class);


	/** The element proxied by {@link #initProxy()} */
	private T proxiedElement;

	/** List of objects to propagate to */
	private List<T> propagateTo = Lists.newArrayList();



	/**
	 * @return The class of {@link #proxiedElement}
	 */
	protected abstract Class<T> getProxiedClass();


	@PostConstruct
	@SuppressWarnings("unchecked")
	private void initProxy() {

		// initialize the proxy
		proxiedElement = (T) Proxy.newProxyInstance(
				getProxiedClass().getClassLoader(),
				new Class[] { getProxiedClass() },
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args)
							throws Throwable {
						synchronized (propagateTo) {
							log.trace("{propagating " + method.getName() + " to " + propagateTo.size() + " elements}");
							for (T o : propagateTo) {
								method.invoke(o, args);
							}
						}
						return null;
					}
				});

	}


	/**
	 * Register a propagant to {@link #propagateTo}.
	 * 
	 * @param propagant
	 */
	public void registerListener(T propagant) {
		synchronized (propagateTo) {
			propagateTo.add(propagant);
		}
	}


	/**
	 * Unregister a propagant from {@link #propagateTo}.
	 * 
	 * @param propagant
	 */
	public void unregisterListener(JLFUListener propagant) {
		synchronized (propagateTo) {
			propagateTo.remove(propagant);
		}
	}


	/**
	 * @return the propagator.
	 */
	public T getPropagator() {
		return proxiedElement;
	}
}
