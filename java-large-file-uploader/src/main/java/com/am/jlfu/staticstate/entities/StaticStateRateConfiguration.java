package com.am.jlfu.staticstate.entities;


import java.io.Serializable;



/**
 * Configuration for the upload.
 * 
 * @author antoinem
 * 
 */
public class StaticStateRateConfiguration
		implements Serializable {

	/**
	 * The rate of the client in kilo bytes.<br>
	 * Minimum should be set to 10kb/s.<br>
	 * Maximum is {@link Long#MAX_VALUE}<br>
	 * */
	private Integer rateInKiloBytes;



	public StaticStateRateConfiguration() {
		super();
	}


	public Integer getRateInKiloBytes() {
		return rateInKiloBytes;
	}


	public void setRateInKiloBytes(Integer rateInKiloBytes) {
		this.rateInKiloBytes = rateInKiloBytes;
	}


}
