package com.webcrawler.app.interfaces;

/**
 * interface to notify when crawl Queue limit is reached
*/
public interface IPoolReportable {
	/**
	 * function to notify when Queue pool limit reached
	*/
	public void onPoolSizeReached();
}
