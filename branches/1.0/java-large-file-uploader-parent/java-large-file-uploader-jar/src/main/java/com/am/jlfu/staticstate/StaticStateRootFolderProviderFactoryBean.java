package com.am.jlfu.staticstate;


import java.io.File;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;



@Component
public class StaticStateRootFolderProviderFactoryBean
		implements FactoryBean<StaticStateRootFolderProvider> {

	@Autowired(required = false)
	WebApplicationContext webApplicationContext;

	@Autowired(required = false)
	StaticStateRootFolderProvider stateRootFolderProvider;

	public static final String DEFAULT_FOLDER_NAME = "JavaFileUploaderManager";



	@Override
	public StaticStateRootFolderProvider getObject()
			throws Exception {
		if (stateRootFolderProvider == null) {
			return new StaticStateRootFolderProvider() {

				@Override
				public File getRootFolder() {
					String realPath = webApplicationContext.getServletContext().getRealPath("/" + DEFAULT_FOLDER_NAME);
					File file = new File(realPath);
					// create if non existent
					if (!file.exists()) {
						file.mkdirs();
					}
					// if existent but a file, runtime exception
					else {
						if (file.isFile()) {
							throw new RuntimeException(file.getAbsolutePath() +
									" is a file. The default root folder provider uses this path to store the files. Consider using a specific root folder provider or delete this file.");
						}
					}
					return file;
				}
			};
		}
		else {
			return stateRootFolderProvider;
		}
	}


	@Override
	public Class<?> getObjectType() {
		return StaticStateRootFolderProvider.class;
	}


	@Override
	public boolean isSingleton() {
		return true;
	}


}
