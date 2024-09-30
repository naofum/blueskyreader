package nl.siegmann.epublib.epub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.service.MediatypeService;

public class EpubReaderTest2 {

	@Test
	public void testEPUB3() throws IOException {
		File file = new File(".");
		System.out.println(file.getAbsolutePath());
		Book readBook = new EpubReader().readEpub(new FileInputStream("./src/test/resources/epub3/valentin-hauy.epub"));
		System.out.println("");
		assertNull(readBook.getCoverImage());
	}

	@Test
	public void testEPUB3_2() throws IOException {
		Book readBook = new EpubReader().readEpub(new FileInputStream("./src/test/resources/epub3/valentin-hauy.epub"));
		System.out.println("");
		Resources resources = readBook.getResources();
		assertEquals(4, resources.size());
		assertEquals("valentin.jpg", resources.getById("opf2").getHref());
	}

}
