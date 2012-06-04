package com.am.jlfu.staticstate;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.identifier.IdentifierProvider;



@Component
public class StaticStateIdentifierManager {

	@Autowired
	IdentifierProvider identifierProvider;

	@Autowired
	RequestComponentContainer requestComponentContainer;



	public String getIdentifier() {
		return identifierProvider.getIdentifier(requestComponentContainer.getRequest(), requestComponentContainer.getResponse()).toString();
	}


	public void clearIdentifier() {
		identifierProvider.clearIdentifier(requestComponentContainer.getRequest(), requestComponentContainer.getResponse());
	}


}
