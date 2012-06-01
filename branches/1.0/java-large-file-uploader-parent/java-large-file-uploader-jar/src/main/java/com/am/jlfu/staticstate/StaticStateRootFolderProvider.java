package com.am.jlfu.staticstate;


import java.io.File;



/**
 * Provides the root folder in which files will be uploaded.<br>
 * This interface has to be implemented by a spring bean.
 * 
 * @author antoinem
 * 
 */
public interface StaticStateRootFolderProvider {

	File getRootFolder();
}
