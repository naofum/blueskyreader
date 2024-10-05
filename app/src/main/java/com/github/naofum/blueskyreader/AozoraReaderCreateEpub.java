package com.github.naofum.blueskyreader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;

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
    static private RadioButton radioButtonName3;
    static private CheckBox checkBox;
    static private CheckBox checkBox2;
    static private ProgressBar progress;
    static private TextView textView;
    static private Button button;

    final Handler handler = new Handler();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.epub);

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
        radioButtonDir1.setText(getFilesDir() + DOWNLOAD_PATH);
        radioButtonDir2 = (RadioButton)findViewById(R.id.radioButtonDir2);
        radioButtonDir2.setText(getFilesDir() + DOWNLOAD_PATH2);
        radioButtonDir1.setEnabled(false);
        radioButtonDir2.setEnabled(false);
        radioButtonName1 = (RadioButton)findViewById(R.id.radioButtonName1);
        radioButtonName1.setText(fileName);
        radioButtonName2 = (RadioButton)findViewById(R.id.radioButtonName2);
        radioButtonName2.setText(authorName + "_" + fileName);
        radioButtonName3 = (RadioButton)findViewById(R.id.radioButtonName3);
        radioButtonName3.setText(authorName + "_" + worksName + "_" + fileName);
        checkBox = (CheckBox)findViewById(R.id.checkBox);
        checkBox2 = (CheckBox)findViewById(R.id.checkBox2);
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
                if (values[0] < 0) {
                    progress.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                } else {
                    progress.setProgress(values[0]);
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                textView.setText(R.string.parsing_xhtml);
                button.setEnabled(false);
                progress.getProgressDrawable().clearColorFilter();
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
                int idnum = 1;

                // create work directory
                File file = new File(getFilesDir() + DOWNLOAD_PATH);
                file.mkdirs();
                publishProgress(0);

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
                            reader = new BufferedReader(new InputStreamReader(in, "Shift_JIS"));
                        }
                    } else {
                        reader = new BufferedReader(new InputStreamReader(in, charEncoding));
                    }
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFilesDir() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8"));

                    String line;
                    Pattern card_pattern = Pattern.compile("(https://www.aozora.gr.jp/cards/\\d+/files)/\\d+_\\d+\\.html");
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
                        if (line.indexOf("href=\"../../default.css\"") >= 0) {
                            line = line.replaceAll("../../default.css", "default.css");
                            retStr = new String();
                            retStr = String.format("%s/%s", base_card, "../../default.css");
                            retList.add(retStr);
                        }
                        if (line.indexOf("<h1 ") >= 0 && line.indexOf(" id=") < 0) {
                            line = line.replaceAll("<h1 ", "<h1 id=\"ops" + String.valueOf(idnum++) + "\" ");
                        }
                        if (checkBox2.isChecked()) {
                            if (line.indexOf("<div ") >= 0 && line.indexOf(" id=") < 0) {
                                line = line.replaceAll("<div ", "<div id=\"id_" + String.valueOf(idnum++) + "\" ");
                            }
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

                            if (checkBox.isChecked()) {
                                Pattern gaiji_pattern = Pattern.compile("<img src=\"" + fileName + "\" .+? class=\"gaiji\" />");
                                Matcher gaiji_matcher = gaiji_pattern.matcher(line);
                                if (gaiji_matcher.find()) {
                                    String replace = gaiji_matcher.group();
                                    if (ConvertUtil.convert(fileName) != null) {
                                        line = StringUtils.replace(line, replace, ConvertUtil.convert(fileName));
//                                        line = line.replaceAll(replace, ConvertUtil.convert(fileName));
                                    }
                                }
                            }
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
                    publishProgress(-1);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    publishProgress(-1);
                }

                return retList;
            }

        };

        task.execute(urlStr);
    }

    private List<TOCReference> createTOC(String urlStr) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(R.string.creating_toc);
            }
        });
        List<TOCReference> references = new ArrayList<TOCReference>();
        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(getFilesDir() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8");
            Resource resource = new Resource(in, "chapter1.html");

            Document document = Jsoup.parse(new FileInputStream(getFilesDir() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8", getFilesDir() + "/");
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
                if (values[0] < 0) {
                    progress.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                } else {
                    progress.setProgress(values[0]);
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                textView.setText(R.string.creating_epub);
                button.setEnabled(false);
                progress.getProgressDrawable().clearColorFilter();
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                textView.setText(R.string.created_epub);
                button.setEnabled(true);
                button.setText(R.string.close);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
//                        if (getOnBackPressedDispatcher().hasEnabledCallbacks()) {
//                            getOnBackPressedDispatcher().onBackPressed();
//                        }
                    }
                });
            }

            private void createBook(String... urlStr) {
                List<TOCReference> references = createTOC(urlStr[0]);
                TableOfContents tableOfContents = new TableOfContents(references);

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
                                url = new URL(urlStr[0]);
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

                try {
                    InputStreamReader in = new InputStreamReader(new FileInputStream(getFilesDir() + DOWNLOAD_PATH + DOWNLOAD_TMP), "UTF-8");
                    Book book = new Book();
                    book.getMetadata().addAuthor(new Author(authorName.trim()));
                    book.getMetadata().addTitle(worksName);
                    book.getMetadata().addPublisher("青空文庫");
                    book.getMetadata().setLanguage("ja");
                    Map<QName, String> modified = new HashMap<>();
                    Date current = new Date();
                    SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    QName qName = new QName("dcterms:modified");
                    modified.put(qName, sf.format(current));
                    book.getMetadata().setOtherProperties(modified);
                    List<String> descriptions = new ArrayList<String>();
                    descriptions.add(getString(R.string.description1));
                    descriptions.add(getString(R.string.description2));
                    book.getMetadata().setDescriptions(descriptions);
//            book.getMetadata().setCoverImage(new Resource(Simple1.class.getResourceAsStream("/book1/test_cover.png"), "cover.png"));
                    Resource resource = new Resource(in, "chapter1.html");
                    resource.setMediaType(MediatypeService.XHTML);
//            resource.setInputEncoding("Shift_JIS");
                    book.addSection(getString(R.string.section_body), resource);
                    TOCReference tocReference = new TOCReference(worksName, resource);
                    tableOfContents.addTOCReference(tocReference);
                    book.setTableOfContents(tableOfContents);
                    for (int i = 1; i < urlStr.length; i++) {
                        Integer progress = 50 + i * 25 / urlStr.length;
                        publishProgress(progress);
                        URL urlRes = new URL(urlStr[i]);
                        HttpURLConnection httpRes = (HttpURLConnection) urlRes.openConnection();
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            httpRes = (HttpsURLConnection) httpRes;
                            ((HttpsURLConnection) httpRes).setSSLSocketFactory(sslContext.getSocketFactory());
                        }
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
//                            e.printStackTrace();
                            throw new RuntimeException(e);
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
                    } else if (radioButtonName2.isChecked()) {
                        epubName = radioButtonName2.getText().toString();
                    } else {
                        epubName = radioButtonName3.getText().toString();
                    }
                    EpubWriter epubWriter = new EpubWriter();
                    epubWriter.write(book, new FileOutputStream(getFilesDir() + DOWNLOAD_PATH + "/" + epubName));
                    in.close();

                    publishProgress(90);
                    boolean needRetry = false;
                    try {
                        storeMedia(getFilesDir() + DOWNLOAD_PATH + File.separator, epubName, epubName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        needRetry = true;
                    }

                    if (needRetry) {
                        Log.d("CreateEpub", "Retrying store media");
                        final String epub = epubName;
                        MediaScannerConnection.scanFile(getApplicationContext(), new String[] {Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()}, new String[]{"application/epub+zip"}, new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                storeMedia(getFilesDir() + DOWNLOAD_PATH + File.separator, epub, epub);
                            }
                        });
                    }

                    publishProgress(100);
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO
                    publishProgress(-1);
                }

            }

            private void storeMedia(String inputPath, String inputFile, String outputPath) {
                String download_path = Environment.DIRECTORY_DOWNLOADS.substring(Environment.DIRECTORY_DOWNLOADS.lastIndexOf("/") + 1);
                ContentValues contentValues = new ContentValues();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Audio.Media.RELATIVE_PATH, download_path);
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    contentValues.put(MediaStore.Audio.Media.ALBUM, outputPath);
                    contentValues.put(MediaStore.Audio.Media.TITLE, inputFile);
                }
                contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, inputFile);
                contentValues.put(MediaStore.Audio.Media.MIME_TYPE, "application/epub+zip");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 1);
                }
                ContentResolver resolver = getContentResolver();
                Uri collection;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    collection = MediaStore.Files.getContentUri(
                            MediaStore.VOLUME_EXTERNAL_PRIMARY);
                } else {
                    collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Audio.Media.DATA, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), inputPath).getAbsolutePath() + "/" + outputPath);
                }
                Uri item = resolver.insert(collection, contentValues);

                try {
                    assert item != null;
                    try (OutputStream out = getContentResolver().openOutputStream(item)) {
                        InputStream in = new FileInputStream(inputPath + inputFile);

                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        in = null;

                        // delete the original file
                        new File(inputPath + inputFile).delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    publishProgress(-1);
                }

                contentValues.clear();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0);
                    resolver.update(item, contentValues, null, null);
                }
            }



        };

        task.execute(url);
    }

}
