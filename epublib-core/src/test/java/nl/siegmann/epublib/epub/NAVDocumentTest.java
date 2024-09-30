package nl.siegmann.epublib.epub;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.IOUtil;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NAVDocumentTest {

    byte[] navData;

    public NAVDocumentTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        navData = IOUtil.toByteArray(new FileInputStream(new File("src/test/resources/navigation.xml")));
    }

    @After
    public void tearDown() {
    }

    private void addResource(Book book, String filename) {
        Resource chapterResource = new Resource("id1", "Hello, world !".getBytes(), filename, MediatypeService.XHTML);
        book.addResource(chapterResource);
        book.getSpine().addResource(chapterResource);
    }
    
    /**
     * Test of read method, of class NCXDocument.
     */
    @Test
    public void testReadWithNonRootLevelTOC() {
        
        // If the tox.ncx file is not in the root, the hrefs it refers to need to preserve its path.
        Book book = new Book();
//        Resource navResource = new Resource(navData, "xhtml/toc.ncx");
        Resource navResource = new Resource(navData, "xhtml/navigation.html");
        addResource(book, "xhtml/chapter1.html");
        addResource(book, "xhtml/chapter2.html");
        addResource(book, "xhtml/chapter2_1.html");
        addResource(book, "xhtml/chapter3.html");

        book.setNavResource(navResource);
        book.getSpine().setTocResource(navResource);

        NAVDocument.read(book, new EpubReader());
        assertEquals("xhtml/chapter1.html", book.getTableOfContents().getTocReferences().get(0).getCompleteHref());
    }
}
