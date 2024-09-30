package nl.siegmann.epublib.epub;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.GuideReference;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.util.CollectionUtil;

public class WinTest {

	@Test
	public void testBook1() throws IOException {
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setValidating(true);
			XmlSerializer serializer = factory.newSerializer();

			Writer output = new StringWriter(1000);
			serializer.setOutput(output);
			serializer.startTag("", "html");
			serializer.endTag("", "html");
			String out = output.toString();


//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		XmlSerializer serializer = factory.newDocumentBuilder().;
			Assert.assertEquals("<html />", out);
		} catch	(Exception e) {
			e.printStackTrace();
		}
		
	    }
}
