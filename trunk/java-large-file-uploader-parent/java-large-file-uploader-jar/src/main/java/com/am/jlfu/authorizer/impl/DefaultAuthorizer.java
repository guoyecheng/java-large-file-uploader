package com.am.jlfu.authorizer.impl;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.am.jlfu.authorizer.Authorizer;
import com.am.jlfu.fileuploader.web.UploadServletAction;



/**
 * Default authorizer that always returns true.
 * 
 * @author antoinem
 * 
 */
@Component
public class DefaultAuthorizer
		implements Authorizer {

	@Override
	public void getAuthorization(HttpServletRequest request, UploadServletAction action, UUID clientId, UUID optionalFileId) {
		// by default, all calls are authorized
	}

}
