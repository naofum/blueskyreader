package com.github.naofum.blueskyreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class AozoraBunkoViewer extends AppCompatActivity {

	public static final String KEY_AUTHORID = "Viewer_AUTHORID";
	public static final String KEY_WORKSID  = "Viewer_WORKSID";
	public static final String KEY_LOCATION = "Viewer_Location";
	public static final String KEY_AUTHORNAME = "Viewer_AUTHORNAME";
	public static final String KEY_WORKSNAME = "Viewer_WORKSNAME";
	public static final String KEY_BOOKMARKED = "Viewer_BOOKMARKED";

	private static final int INDEX_ID = 0;

	private long authorId, worksId;
	private String authorName, worksName;
	private String xhtmlUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		long authorId, worksId;
//		String authorName, worksName;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewer);
		
		// Pick Up bundle.
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			boolean bookmarked = extras.getBoolean (AozoraBunkoViewer.KEY_BOOKMARKED);

			authorId = extras.getLong(AozoraBunkoViewer.KEY_AUTHORID);
			worksId = extras.getLong(AozoraBunkoViewer.KEY_WORKSID);
			authorName = extras.getString (AozoraBunkoViewer.KEY_AUTHORNAME);
			worksName  = extras.getString(AozoraBunkoViewer.KEY_WORKSNAME);
			// authorId become 0 when authorId stored not correctly

			// Here edit Activity's label.
			String title = worksName + "/" + authorName;
			setTitle(title);

			WebView webview = (WebView)findViewById(R.id.aozora_webview);
			webview.getSettings().setJavaScriptEnabled(true);
			ProgressBar progressBar = (ProgressBar)findViewById(R.id.viewerProgressBar);

//			String xhtmlUrl;
			if (bookmarked) {
				xhtmlUrl = extras.getString(AozoraBunkoViewer.KEY_LOCATION);
				webview.loadUrl(xhtmlUrl);
			} else {
				String location = extras.getString(AozoraBunkoViewer.KEY_LOCATION);
				//TODO check?
				if (!location.startsWith("http")) {
					location = "https://www.aozora.gr.jp/cards/" + location;
				}
				progressBar.setVisibility(View.VISIBLE);
				getXHTMLURLStringAsync(location, authorId, worksId, url -> {
					progressBar.setVisibility(View.GONE);
					xhtmlUrl = url;
					AozoraReaderBookmarksDbAdapter mDbAdapter = new AozoraReaderBookmarksDbAdapter(this);
					mDbAdapter.open();
					mDbAdapter.insertInfo(authorName, authorId, worksName, worksId, xhtmlUrl);
					mDbAdapter.close();
					webview.loadUrl(xhtmlUrl);
				});
			}
		}
		
	}

	interface OnXHTMLUrlFetchedListener {
		void onFetched(String url);
	}

	private void getXHTMLURLStringAsync(String urlStr, long authorId, long worksId,
	                                    OnXHTMLUrlFetchedListener listener) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler handler = new Handler(Looper.getMainLooper());
		executor.execute(() -> {
			String result = getXHTMLURLString(urlStr, authorId, worksId);
			handler.post(() -> listener.onFetched(result));
		});
		executor.shutdown();
	}

	private String getXHTMLURLString(String urlStr, long authorId, long worksId) {
		String retStr = null;

		HttpURLConnection http = null;
		try {
			URL url = new URL(urlStr);
			http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod("GET");
			http.setConnectTimeout(15000);
			http.setReadTimeout(30000);
			http.connect();

			String charEncoding = http.getContentEncoding();
			String encoding;
			if (charEncoding == null) {
				String contentType = http.getContentType();
				Pattern contentTypePattern = Pattern.compile(".+charset=(.+)");
				Matcher contentTypeMatcher = contentTypePattern.matcher(contentType);
				if (contentTypeMatcher.find()) {
					encoding = contentTypeMatcher.group(1);
				} else {
					encoding = "UTF-8";
				}
			} else {
				encoding = charEncoding;
			}

			try (InputStream in = http.getInputStream();
				 BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding))) {

				String line;
				/*
				 * To match the following lines:
				 *［<a href="#download">ファイルのダウンロード</a>｜<a href="./files/236_19996.html">いますぐXHTML版で読む</a>］
				 *
				 */
				Pattern xhtml_pattern = Pattern.compile("<a href=\"\\./(files/\\d+_\\d+\\.html)\">いますぐXHTML版で読む</a>");
				while ((line = reader.readLine()) != null) {
					Matcher xhtml_matcher = xhtml_pattern.matcher(line);
					if (xhtml_matcher.find()) {
						String xhtmlLoc = xhtml_matcher.group(1);
						Pattern card_pattern = Pattern.compile("(https://www.aozora.gr.jp/cards/\\d+)/card\\d+\\.html");
						Matcher card_matcher = card_pattern.matcher(urlStr);
						if (card_matcher.find()) {
							String base_card = card_matcher.group(1);
							retStr = String.format("%s/%s", base_card, xhtmlLoc);
						}
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (http != null) {
				http.disconnect();
			}
		}

		if (retStr == null) {
			retStr = urlStr;
		}

		return retStr;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean retval;
		retval = super.onCreateOptionsMenu(menu);
		menu.add(0, INDEX_ID, 0, R.string.menu_create_epub);
		return retval;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean retval = false;
		retval = super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case INDEX_ID:
				startCreateEpubActivity();
				break;
		}
		return retval;
	}

	private void startCreateEpubActivity() {
		Intent i = new Intent(this, AozoraReaderCreateEpub.class);
		i.putExtra(AozoraBunkoViewer.KEY_AUTHORID,   authorId);
		i.putExtra(AozoraBunkoViewer.KEY_AUTHORNAME, authorName);
		i.putExtra(AozoraBunkoViewer.KEY_WORKSID, worksId);
		i.putExtra(AozoraBunkoViewer.KEY_WORKSNAME , worksName);
		i.putExtra(AozoraBunkoViewer.KEY_LOCATION, xhtmlUrl);
		i.putExtra(AozoraBunkoViewer.KEY_BOOKMARKED, false);
		startActivity(i);
	}

}
