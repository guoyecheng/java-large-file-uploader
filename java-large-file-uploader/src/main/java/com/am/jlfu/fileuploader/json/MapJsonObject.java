package com.am.jlfu.fileuploader.json;


import java.util.HashMap;



public class MapJsonObject
		implements JsonObject {

	/**
	 * generated id
	 */
	private static final long serialVersionUID = 1815625862539981019L;

	/**
	 * Value.
	 */
	private HashMap<String, String> value;



	public MapJsonObject(HashMap<String, String> value) {
		super();
		this.value = value;
	}


	public MapJsonObject() {
		super();
	}


	public HashMap<String, String> getValue() {
		return value;
	}


	public void setValue(HashMap<String, String> value) {
		this.value = value;
	}


}
