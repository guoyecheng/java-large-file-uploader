package com.am.jlfu.fileuploader.exception;


import java.util.UUID;

import com.am.jlfu.fileuploader.web.UploadServletAction;



public class AuthorizationException extends Exception {

	public AuthorizationException(UploadServletAction actionByParameterName, UUID clientId, UUID optionalFileId) {
		super("User " + clientId + " is not authorized to perform " + actionByParameterName + (optionalFileId != null ? " on " + optionalFileId : ""));
	}


}
