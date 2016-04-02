package com.github.naofum.blueskyreader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubWriter;

public class AozoraReaderCreateEpub extends AppCompatActivity {

    public static final String KEY_AUTHORID = "Viewer_AUTHORID";
    public static final String KEY_WORKSID  = "Viewer_WORKSID";
    public static final String KEY_LOCATION = "Viewer_Location";
    public static final String KEY_AUTHORNAME = "Viewer_AUTHORNAME";
    public static final String KEY_WORKSNAME = "Viewer_WORKSNAME";
    public static final String KEY_BOOKMARKED = "Viewer_BOOKMARKED";

    static final String DOWNLOAD_PATH = "/Download";
    static final String DOWNLOAD_PATH2 = "/Books";
    static final String DOWNLOAD_TMP = "/bluesky.tmp";

    static long authorId, worksId;
    static String authorName, worksName;
    static String xhtmlUrl;
    static String fileName;

    static private RadioButton radioButtonDir1;
    static private RadioButton radioButtonDir2;
    static private RadioButton radioButtonName1;
    static private RadioButton radioButtonName2;
    static private ProgressBar progress;
    static private TextView textView;
    static private Button button;

    InterstitialAd mInterstitialAd;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.epub);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mInterstitialAd.loadAd(adRequest);

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        });

        progress = (ProgressBar)findViewById(R.id.progressBar);
        textView = (TextView)findViewById(R.id.textView);

        // Pick Up bundle.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            authorId = extras.getLong(AozoraBunkoViewer.KEY_AUTHORID);
            worksId = extras.getLong(AozoraBunkoViewer.KEY_WORKSID);
            authorName = extras.getString(AozoraBunkoViewer.KEY_AUTHORNAME);
            worksName = extras.getString(AozoraBunkoViewer.KEY_WORKSNAME);
            boolean bookmarked = extras.getBoolean(AozoraBunkoViewer.KEY_BOOKMARKED);
            xhtmlUrl = extras.getString(AozoraBunkoViewer.KEY_LOCATION);
            String split[] = xhtmlUrl.split("/");
            fileName = split[split.length - 1].replaceAll("html", "epub");
        }

        radioButtonDir1 = (RadioButton)findViewById(R.id.radioButtonDir1);
        radioButtonDir1.setText(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH);
        radioButtonDir2 = (RadioButton)findViewById(R.id.radioButtonDir2);
        radioButtonDir2.setText(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH2);
        radioButtonDir1.setEnabled(false);
        radioButtonDir2.setEnabled(false);
        radioButtonName1 = (RadioButton)findViewById(R.id.radioButtonName1);
        radioButtonName1.setText(fileName);
        radioButtonName2 = (RadioButton)findViewById(R.id.radioButtonName2);
        radioButtonName2.setText(authorName + "_" + fileName);
        button = (Button)findViewById(R.id.button);

        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getResourceURLStringAsync(xhtmlUrl);
            }
        });
    }

    private void getResourceURLStringAsync(String urlStr) {
        AsyncTask<String, Integer, List<String>> task = new AsyncTask<String, Integer, List<String>>(){

            @Override
            protected List<String> doInBackground(String... params) {
                return getResourceURLString(params[0]);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progress.setProgress(values[0]);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                textView.setText(R.string.parsing_xhtml);
                button.setEnabled(false);
            }

            @Override
            protected void onPostExecute(List<String> result) {
                String[] resources = result.toArray(new String[result.size() + 1]);
                if(resources.length > 0) {
                    for (int i = resources.length - 1; i > 0; i--) {
                        resources[i] = resources[i - 1];
                    }
                    resources[0] = xhtmlUrl;
                    createBookAsync(resources);
                } else {
                    // TODO error
                    button.setEnabled(true);
                }
            }

            private List<String> getResourceURLString(String urlStr) {
                String retStr = null;
                List<String> retList = new ArrayList<String>();

                publishProgress(0);
                URL url;
                try {
                    url = new URL(urlStr);
                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
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
                            reader = new BufferedReader(new InputStreamReader(in, "Shift_JIS"));
                        }
                    } else {
                        reader = new BufferedReader(new InputStreamReader(in, charEncoding));
                    }
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8"));

                    String line;
                    Pattern card_pattern = Pattern.compile("(http://www.aozora.gr.jp/cards/\\d+/files)/\\d+_\\d+\\.html");
                    Matcher card_matcher = card_pattern.matcher(urlStr);
                    String base_card = "";
                    if (card_matcher.find()) {
                        base_card = card_matcher.group(1);
                    }
                    Pattern xhtml_pattern = Pattern.compile(" src=\"(.+?)\"");
                    while ((line = reader.readLine()) != null) {
                        if (line.indexOf("<script") >= 0) {
                            continue;
                        }
                        if (line.indexOf("Shift_JIS\"") >= 0) {
                            line = line.replaceAll("Shift_JIS\"", "UTF-8\"");
                        }
                        if (line.indexOf("href=\"../../aozora.css\"") >= 0) {
                            line = line.replaceAll("../../aozora.css", "aozora.css");
                            retStr = new String();
                            retStr = String.format("%s/%s", base_card, "../../aozora.css");
                            retList.add(retStr);
                        }
                        Matcher xhtml_matcher = xhtml_pattern.matcher(line);
                        while (xhtml_matcher.find()) {
                            String xhtmlLoc = xhtml_matcher.group(1);
                            retStr = new String();
                            retStr = String.format("%s/%s", base_card, xhtmlLoc);
                            retList.add(retStr);

                            String[] pathElements = xhtmlLoc.split("/");
                            String fileName = pathElements[pathElements.length - 1];
                            line = line.replaceAll(xhtmlLoc, fileName);
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                    writer.close();
                    reader.close();
                    in.close();
                    http.disconnect();
                    publishProgress(25);
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return retList;
            }

        };

        task.execute(urlStr);
    }

    private List<TOCReference> createTOC(String urlStr) {
        textView.setText(R.string.creating_toc);
        List<TOCReference> references = new ArrayList<TOCReference>();
        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8");
            Resource resource = new Resource(in, "chapter1.html");

            Document document = Jsoup.parse(new FileInputStream(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8", Environment.getExternalStorageDirectory().getPath() + "/");
            Elements elements = document.select(".midashi_anchor");

            int root_level = 3;
            int prev_level = 3;
            int level = 0;
            TOCReference reference = null;
            TOCReference ref1 = null;
            TOCReference ref2 = null;
            for (Element element : elements) {
                Element parent = element.parent();
                level = Integer.valueOf(parent.tagName().substring(1, 2));
                reference = new TOCReference(element.text(), resource, element.id(), new ArrayList<TOCReference>());
                if (level == root_level) {
                    references.add(reference);
                } else if (level == root_level + 1) {
                    ref1 = references.get(references.size() - 1);
                    ref1.getChildren().add(reference);
                } else if (level >= root_level + 2) {
                    ref1 = references.get(references.size() - 1);
                    List<TOCReference> list1 = ref1.getChildren();
                    if (list1.size() > 0) {
                        ref2 = list1.get(list1.size() - 1);
                        ref2.getChildren().add(reference);
                    }
                }
            }

            in.close();

            progress.setProgress(50);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return references;
    }

    private void createBookAsync(String... url) {
        AsyncTask<String, Integer, Void> task = new AsyncTask<String, Integer, Void>(){

            @Override
            protected Void doInBackground(String... params) {
                createBook(params);
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progress.setProgress(values[0]);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                textView.setText(R.string.creating_epub);
                button.setEnabled(false);
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                textView.setText(R.string.created_epub);
                button.setEnabled(true);
            }

            private void createBook(String... urlStr) {
                List<TOCReference> references = createTOC(urlStr[0]);
                TableOfContents tableOfContents = new TableOfContents(references);
                try {
                    InputStreamReader in = new InputStreamReader(new FileInputStream(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8");
                    Book book = new Book();
                    book.getMetadata().addAuthor(new Author(authorName));
                    book.getMetadata().addTitle(worksName);
                    book.getMetadata().setLanguage("ja");
                    List<String> descriptions = new ArrayList<String>();
                    descriptions.add(getString(R.string.description1));
                    descriptions.add(getString(R.string.description2));
                    book.getMetadata().setDescriptions(descriptions);
//            book.getMetadata().setCoverImage(new Resource(Simple1.class.getResourceAsStream("/book1/test_cover.png"), "cover.png"));
                    Resource resource = new Resource(in, "chapter1.html");
//            resource.setInputEncoding("Shift_JIS");
                    book.addSection(getString(R.string.section_body), resource);
                    book.setTableOfContents(tableOfContents);
                    for (int i = 1; i < urlStr.length; i++) {
                        Integer progress = 50 + i * 50 / urlStr.length;
                        publishProgress(progress);
                        URL urlRes = new URL(urlStr[i]);
                        HttpURLConnection httpRes = (HttpURLConnection) urlRes.openConnection();
                        httpRes.setRequestMethod("GET");
                        httpRes.connect();
                        InputStream inRes = null;
                        try {
                            inRes = httpRes.getInputStream();
                            String[] pathElements = urlRes.getPath().split("/");
                            String fileName = pathElements[pathElements.length - 1];
                            if (!fileName.equals("xxxx.png")) {
                                book.getResources().add(new Resource(inRes, fileName));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (inRes != null) {
                                inRes.close();
                            }
                        }
                        httpRes.disconnect();
                    }
                    String epubName = "";
                    if (radioButtonName1.isChecked()) {
                        epubName = radioButtonName1.getText().toString();
                    } else {
                        epubName = radioButtonName2.getText().toString();
                    }
                    EpubWriter epubWriter = new EpubWriter();
                    epubWriter.write(book, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + DOWNLOAD_PATH + "/" + epubName));
                    in.close();
                    publishProgress(100);
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO
                }

            }

        };

        task.execute(url);
    }

}
