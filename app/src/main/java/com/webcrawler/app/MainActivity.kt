package com.webcrawler.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.webcrawler.app.interfaces.ICrawlerReportable
import com.webcrawler.app.interfaces.IPoolReportable
import com.webcrawler.app.queue.URLPool
import com.webcrawler.app.threads.GetLinksService
import com.webcrawler.app.util.Utility

class MainActivity() : Activity(), ICrawlerReportable, IPoolReportable {
    private var mUrl: EditText? = null
    private var mButton1: Button? = null
    private var mCurrentLinkText: TextView? = null
    private var mLinksCountText: TextView? = null
    private var mProcessedLinksCountText: TextView? = null
    private var mPageURL: String? = null
    private var mIsActive = false
    private var mProgressBar: ProgressBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initializing UI elements
        mUrl = findViewById<View>(R.id.urlEditText) as EditText
        mButton1 = findViewById<View>(R.id.beginButton) as Button
        mCurrentLinkText = findViewById<View>(R.id.currentLinkTextView) as TextView
        mLinksCountText = findViewById<View>(R.id.goodLinkTextView) as TextView
        mProcessedLinksCountText = findViewById<View>(R.id.badLinkTextView) as TextView
        URLPool.getInstance().setPoolListener(this)
        mProgressBar = findViewById<View>(R.id.progressBar1) as ProgressBar
    }

    fun beginButtonClicked(view: View?) {
        if (!(Utility.validatURL(
                mUrl!!.text.toString(),
                this
            ) && Utility.isIntenetAvailable(this))
        ) {
            return
        }
        mPageURL = mUrl!!.text.toString()
        if (mIsActive) {
            stopCrawling()
        } else {
            mIsActive = true
            mButton1!!.setText(R.string.cancle_text)
            Toast.makeText(this, "Starting Crawler", Toast.LENGTH_SHORT).show()
            URLPool.getInstance().push(mPageURL)
            startCrawling()
        }
    }

    private fun startCrawling() {
        try {
            /**
             * before reconstructing ThreadPoolExecutor do some validation
             * 1> Was shutdown called with proper flags
             * <a> start crawling flag (mIsActive) should be set and pool size should be less than its limit
            </a> */
            if (ApplicationEx.operationsQueue.isShutdown() && mIsActive) {
                // calling start again
                ApplicationEx.reconstructThreadPool()
            }
            progressStart()
            /**
             * check if pool has URLs to be crawled
             *
             */
            val pageURL: String = URLPool.getInstance().pop()
            if (!TextUtils.isEmpty(pageURL)) {
                Log.v("startCrawling", pageURL)
                val service = GetLinksService(pageURL, this)
                ApplicationEx.operationsQueue.execute(service)
            } else {
                // either pool is empty or pool limit has reached
                Toast.makeText(
                    this,
                    "Either Queue is empty or its size limit reached",
                    Toast.LENGTH_SHORT
                ).show()
                stopCrawling()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.v("startCrawling", e.localizedMessage)
        }
    }

    private fun stopCrawling() {
        // avoid execution when crawling has already been interrupted by crawler itself due to Queue limit reached or empty Queue
        if (!ApplicationEx.operationsQueue.isShutdown()) {
            mIsActive = false
            mButton1!!.setText(R.string.begin_text)
            Toast.makeText(this, "Crawler Shutting down", Toast.LENGTH_SHORT)
                .show()
            ApplicationEx.operationsQueue.shutdownNow()
            ApplicationEx.operationsQueue.getQueue().clear()

            // current link count
            mLinksCountText!!.text = (getString(R.string.links_count).toString() + " "
                    + URLPool.getInstance().getUrlPoolSize())

            // current processed pool count
            mProcessedLinksCountText?.setText(
                    getString(R.string.processed_links_count) + " "
                            + URLPool.getInstance().getUrlProcessedPoolSize()
                )
            Log.e("link count", " count " + URLPool.getInstance().getUrlPoolSize())
            Log.e(
                "processed link count",
                " count " + URLPool.getInstance().getUrlProcessedPoolSize()
            )
            progressStop()
        }
    }

    override fun spiderFoundURL(urlString: String?) {
        val message = Message.obtain()
        message.what = 1
        message.obj = urlString
        crawlerHandler.sendMessage(message)
    }

    override fun spiderURLProcessed(processedURLCount: Int) {
        // Log.v(TAG, "spiderURLProcessed " + processedURLCount);
        val message = Message.obtain()
        message.what = 3
        message.obj = processedURLCount
        crawlerHandler.sendMessage(message)
    }

    override fun spiderLinkCounts(goodLinkCount: Int) {
        // Log.v(TAG, "spiderGoodLinkCounts");
        val message = Message.obtain()
        message.what = 2
        message.obj = goodLinkCount
        crawlerHandler.sendMessage(message)
    }

    var crawlerHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {

            // current URL
            if (msg.what == 1) {
                mCurrentLinkText!!.text = (getString(R.string.current_link).toString() + " "
                        + msg.obj as String)
            }
            // current link count
            if (msg.what == 2) mLinksCountText!!.text =
                (getString(R.string.links_count).toString() + " "
                        + msg.obj)
            // processed link count
            if (msg.what == 3) {
                mProcessedLinksCountText?.setText((getString(R.string.processed_links_count) + " " + msg.obj))
            }

            // pool size reached
            if (msg.what == 4) {
                stopCrawling()
            }
            if (msg.what == 5) {

                // don't call if shutdown has been called, if stopCrawling flag is active and pool size has reached its limit
                if ((!ApplicationEx.operationsQueue.isShutdown()) && (mIsActive) && (!URLPool.getInstance()
                        .hasPoolSizeReached())
                ) {
                    startCrawling()
                }
            }
        }
    }

    override fun finished() {
        Log.v(TAG, "finished")
        val message = Message.obtain()
        message.what = 5
        crawlerHandler.sendMessage(message)
    }

    override fun onPoolSizeReached() {
        Log.v(TAG, "onPoolSizeReached")
        val message = Message.obtain()
        message.what = 4
        crawlerHandler.sendMessage(message)
    }

    /**
     * start progress bar
     *
     */
    private fun progressStart() {
        mProgressBar!!.visibility = View.VISIBLE
    }

    /**
     * stop progress bar
     *
     */
    private fun progressStop() {
        mProgressBar!!.visibility = View.INVISIBLE
    }

    companion object {
        private val TAG = "CrawlerActivity"
    }
}
