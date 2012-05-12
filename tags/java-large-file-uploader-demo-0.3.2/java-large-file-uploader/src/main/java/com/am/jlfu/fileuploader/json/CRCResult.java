package com.am.jlfu.fileuploader.json;

import java.io.Serializable;

public class CRCResult implements Serializable{


	private String value;
	private int read;


	public CRCResult() {
	}

	public String getCrcAsString() {
		return value;
	}


	public void setCrcAsString(String crcAsString) {
		this.value = crcAsString;
	}


	public int getTotalRead() {
		return read;
	}


	public void setTotalRead(int streamLength) {
		this.read = streamLength;
	}
	
}
