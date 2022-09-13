package com.webcrawler.app.threads;

import android.text.TextUtils;
import android.util.Log;

import com.webcrawler.app.HTTPRequest;
import com.webcrawler.app.entities.CrawlLinks;
import com.webcrawler.app.interfaces.ICrawlerReportable;

public class GetLinksService implements Runnable{
	private static final String TAG = "GetLinksService";
	private String mURL;
	private ICrawlerReportable report;
	private HTTPRequest mRequest;
	private CrawlLinks mCrawlLinks;

	public GetLinksService() {
		//Default constructor
	}
	
	/**
	 * A flag that indicates whether this process should be canceled
	 */
	protected boolean cancel = false;
	
	public GetLinksService(String url, ICrawlerReportable report) {
		this.mURL = url;
		this.report = report;
		mRequest = new HTTPRequest(mURL);
	}
	
	public void run()
	{
		mCrawlLinks = new CrawlLinks(report);
		try{	
			processURL(mURL);
		}catch (Exception e) {
			e.printStackTrace();
			Log.e("run", e.getLocalizedMessage());
		}
		
		report.finished();
	}

	/**
	 * Called internally to process a URL
	 * 
	 * @param
	 */
	public void processURL(String urlString) {
		try {
			report.spiderFoundURL(urlString);
			getWebPage(urlString);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("processURL", e.getLocalizedMessage());
		}
	}
	
	/**
	 * utility function to validate http string response 
	 *@param1 String: URL
	 */
	private void getWebPage(String url) {
		String response = invokeService(url);
		  if (!TextUtils.isEmpty(response)) {
				mCrawlLinks.extractLinks(response, response, url.toString());
		}
		}
	
	/**
	 * function which gets web content
	 *@param1 String: URL
	 */
	private String invokeService(String url) {
		String response = null;
		try
		{
			mRequest.addHeader("Accept-Encoding", "gzip");
			mRequest.execute(HTTPRequest.RequestMethod.GET);
			response = mRequest.getResponseString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		} 
		return response;
	}
	
	
	

	
}