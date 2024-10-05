package com.github.naofum.blueskyreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class AozoraBunkoViewer extends AppCompatActivity {

	public static final String KEY_AUTHORID = "Viewer_AUTHORID";
	public static final String KEY_WORKSID  = "Viewer_WORKSID";
	public static final String KEY_LOCATION = "Viewer_Location";
	public static final String KEY_AUTHORNAME = "Viewer_AUTHORNAME";
	public static final String KEY_WORKSNAME = "Viewer_WORKSNAME";
	public static final String KEY_BOOKMARKED = "Viewer_BOOKMARKED";

	private static final int INDEX_ID = 0;

	static long authorId, worksId;
	static String authorName, worksName;
	static String xhtmlUrl;

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

//			String xhtmlUrl;
			if (bookmarked == true) {
				xhtmlUrl = extras.getString(AozoraBunkoViewer.KEY_LOCATION); 
			} else {
				String location = extras.getString(AozoraBunkoViewer.KEY_LOCATION);
				//TODO check?
				if (!location.startsWith("http")) {
					location = "https://www.aozora.gr.jp/cards/" + location;
				}
				xhtmlUrl = getXHTMLURLStringAsync(location, authorId, worksId);
				AozoraReaderBookmarksDbAdapter mDbAdapter = new AozoraReaderBookmarksDbAdapter(this);
				mDbAdapter.open();
				mDbAdapter.insertInfo(authorName, authorId, worksName, worksId, xhtmlUrl);
				mDbAdapter.close();
			}
			
			// Here edit Activity's label.
			String title = worksName + "/" + authorName;
			setTitle(title);

			WebView webview = (WebView)findViewById(R.id.aozora_webview);
			webview.getSettings().setJavaScriptEnabled(true);
			//TODO for old Android compatibility
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
				webview.setWebViewClient(new WebViewClient() {
					@Override
					public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
						AlertDialog.Builder builder = new AlertDialog.Builder(AozoraBunkoViewer.this);
						builder.setMessage(getString(R.string.ssl_error));
						builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								handler.proceed();
							}
						});
						builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								handler.cancel();
							}
						});
						builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
							@Override
							public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
								if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
									handler.cancel();
									dialog.dismiss();
									return true;
								}
								return false;
							}
						});
						AlertDialog dialog = builder.create();
						dialog.show();
					}
				});
			}
			webview.loadUrl(xhtmlUrl);
		}
		
	}

	private String getXHTMLURLStringAsync(String urlStr, long authorId, long worksId) {
		AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>(){

			@Override
			protected String doInBackground(String... params) {
				return getXHTMLURLString(params[0], Long.valueOf(params[1]), Long.valueOf(params[2]));
			}

		};
		try {
			return task.execute(urlStr, String.valueOf(authorId), String.valueOf(worksId)).get();
		} catch (InterruptedException e) {
			return urlStr;
		} catch (ExecutionException e) {
			return urlStr;
		}
	}

	private String getXHTMLURLString(String urlStr, long authorId, long worksId) {
		String retStr = null;

		//TODO for old Android compatibility
		SSLContext sslContext = null;
		try {
			TrustManager[] tm = {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

						}

						@Override
						public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
					}
			};
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tm, null);
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String s, SSLSession sslSession) {
					URL url = null;
					try {
						url = new URL(urlStr);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					return (url == null ? false : url.getHost().equalsIgnoreCase(s));
				}
			});
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		URL url;
		try {
			url = new URL(urlStr);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
				http = (HttpsURLConnection) http;
				((HttpsURLConnection) http).setSSLSocketFactory(sslContext.getSocketFactory());
			}
			http.setRequestMethod("GET");
			http.connect();
			InputStream in = http.getInputStream();
			String charEncoding = http.getContentEncoding();
			BufferedReader reader;
			if (charEncoding == null) {
				String contentType = http.getContentType();
				Pattern contentTypePattern = Pattern.compile(".+charset=(.+)");
				Matcher contentTypeMatcher = contentTypePattern.matcher(contentType);
				if (contentTypeMatcher.find()) {
					String matchType = contentTypeMatcher.group(1);
					reader = new BufferedReader(new InputStreamReader(in, matchType));
				} else {
//					reader = new BufferedReader(new InputStreamReader(in, "EUC-JP"));
					reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				}
			} else {
				reader = new BufferedReader(new InputStreamReader(in, charEncoding));
			}

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
						retStr = new String();
						retStr = String.format("%s/%s", base_card, xhtmlLoc);
					}
				}
			}
			reader.close();
			in.close();
			http.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
