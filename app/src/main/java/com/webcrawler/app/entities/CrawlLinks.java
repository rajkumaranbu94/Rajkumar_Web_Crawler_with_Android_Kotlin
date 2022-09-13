package com.webcrawler.app.entities;

import android.text.TextUtils;
import android.util.Log;

import com.webcrawler.app.ApplicationEx;
import com.webcrawler.app.interfaces.ICrawlerReportable;
import com.webcrawler.app.queue.URLPool;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

public class CrawlLinks {
	private ICrawlerReportable report;
	private static final String TAG = "CrawlLinks";

	public CrawlLinks() {

	}

	public CrawlLinks(ICrawlerReportable report) {
		this.report = report;
	}

	/**
	 * function which extracts links form web page
	 * 
	 * @param1 String: Web page
	 * @param2 String: Web page
	 * @param3 String: URL of the page to be crawled
	 */
	public void extractLinks(String rawPage, String page, String url) {
		Log.v("extractLinks", url);
		int index = 0;
		while ((index = page.indexOf("<a ", index)) != -1) {
			int tagEnd = page.indexOf(">", index);
			if ((index = page.indexOf("href", index)) == -1)
				break;
			if ((index = page.indexOf("=", index)) == -1)
				break;
			int endTag = page.indexOf("</a", index);
			String remaining = rawPage.substring(++index);
			StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\"'>#");
			String strLink = st.nextToken();

			if((TextUtils.equals(strLink, "javascript:void(0)"))||(TextUtils.equals(strLink, "javascript:void(0);"))){
				// avoiding javscript calls
				continue;
			}
			if (!checkLinks(strLink)) {
				strLink = makeAbsoluteURL(url, strLink);
			}
			if (!TextUtils.isEmpty(strLink)) {
				addLink(strLink);
			}
		}
		report.spiderURLProcessed(URLPool.getInstance()
				.getUrlProcessedPoolSize());
	}

	private void addLink(String url) {
		if(!ApplicationEx.operationsQueue.isShutdown()){
			
		if ((URLPool.getInstance().push(url))&&(!URLPool.getInstance().hasPoolSizeReached())) {
			report.spiderFoundURL(url);
			report.spiderLinkCounts(URLPool.getInstance().getUrlPoolSize());
			// Log.v("addLink", url);
		} 
		}
		
		
	}

	/**
	 * function which validate links if it is relative links returns false for
	 * further processing else true
	 * 
	 * @param1 String: URL
	 * 
	 */
	private boolean checkLinks(String links) {
		if (links.startsWith("mailto:"))
			return true;
		if (links.startsWith("http://"))
			return true;
		if (links.startsWith("https://"))
			return true;
		if (links.startsWith("ftp://"))
			return true;
		if (links.startsWith("news://"))
			return true;
		return false;
	}

	/**
	 * function which constructs complete URL
	 * 
	 * @param1 String: base URL
	 * @param2 String: relative URL
	 */
	private String makeAbsoluteURL(String base, String relativeURL) {
		URL url = null;
		String stringURL = null;
	//	Log.d(base, relativeURL);
		try {
			url = new URL(base);
			url = new URL(url, relativeURL);
			stringURL = url.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Log.e("makeAbsoluteURL", e.getLocalizedMessage());
		}

		return stringURL;
	}

}
