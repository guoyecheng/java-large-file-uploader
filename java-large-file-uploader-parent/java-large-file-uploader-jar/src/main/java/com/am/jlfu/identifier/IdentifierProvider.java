package com.am.jlfu.identifier;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.am.jlfu.identifier.impl.DefaultIdentifierProvider;



/**
 * Provides identification for JLFU API.
 * 
 * @author antoinem
 * @see DefaultIdentifierProvider
 * 
 */
public interface IdentifierProvider {

	/**
	 * Retrieves the client identifier.<br>
	 * 
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * 
	 * @return the unique identifier identifying this client/job
	 */
	UUID getIdentifier(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);


	/**
	 * Removes the identifier.
	 * 
	 * @param request
	 * @param response
	 */
	void clearIdentifier(HttpServletRequest request, HttpServletResponse response);

}
