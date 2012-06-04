package com.am.jlfu.fileuploader.exception;


import com.am.jlfu.fileuploader.web.UploadServletAction;



public class AuthorizationException extends Exception {

	public AuthorizationException(UploadServletAction actionByParameterName, String identifier, String fileId) {
		super("User " + identifier + " is not authorized to perform " + (actionByParameterName + fileId != null ? " on " + fileId : ""));
	}


}
