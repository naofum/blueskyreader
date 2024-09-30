package nl.siegmann.epublib.epub;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.util.CollectionUtil;

public class EpubWriterTest2 {

	@Test
	public void testBook2() throws IOException {
		// create test book
		Book book = createTestBook();
		
		// write book to byte[]
		byte[] bookData = writeBookToByteArray(book);
			FileOutputStream fileOutputStream = new FileOutputStream("bar.zip");
			fileOutputStream.write(bookData);
			fileOutputStream.flush();
			fileOutputStream.close();
		Assert.assertNotNull(bookData);
		Assert.assertTrue(bookData.length > 0);
		
		// read book from byte[]
		Book readBook = new EpubReader().readEpub(new ByteArrayInputStream(bookData));
		
		// assert book values are correct
		Assert.assertEquals(book.getMetadata().getTitles(), readBook.getMetadata().getTitles());
//		Assert.assertEquals(Identifier.Scheme.ISBN, CollectionUtil.first(readBook.getMetadata().getIdentifiers()).getScheme());
		Assert.assertEquals(CollectionUtil.first(book.getMetadata().getIdentifiers()).getValue(), CollectionUtil.first(readBook.getMetadata().getIdentifiers()).getValue());
		Assert.assertEquals(CollectionUtil.first(book.getMetadata().getAuthors()), CollectionUtil.first(readBook.getMetadata().getAuthors()));
//		Assert.assertEquals(1, readBook.getGuide().getGuideReferencesByType(GuideReference.COVER).size());
//		Assert.assertEquals(5, readBook.getSpine().size());
		Assert.assertEquals(6, readBook.getSpine().size());
		Assert.assertNotNull(book.getCoverPage());
		Assert.assertNotNull(book.getCoverImage());
		Assert.assertEquals(3, readBook.getTableOfContents().size());
			
	}
	
	private Book createTestBook() throws IOException {
		Book book = new Book();
		
		book.getMetadata().addTitle("Epublib test book 1");
		book.getMetadata().addTitle("test2");
		
		book.getMetadata().addIdentifier(new Identifier(Identifier.Scheme.ISBN, "987654321"));
		book.getMetadata().addAuthor(new Author("Joe", "Tester"));
		book.setCoverPage(new Resource(this.getClass().getResourceAsStream("/book2/cover.html"), "cover.html"));
		book.setCoverImage(new Resource(this.getClass().getResourceAsStream("/book2/cover.png"), "cover.png"));
		book.addSection("Chapter 1", new Resource(this.getClass().getResourceAsStream("/book2/chapter1.html"), "chapter1.html"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book2/book1.css"), "book1.css"));
		TOCReference chapter2 = book.addSection("Second chapter", new Resource(this.getClass().getResourceAsStream("/book2/chapter2.html"), "chapter2.html"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book2/flowers_320x240.jpg"), "flowers.jpg"));
		book.addSection(chapter2, "Chapter 2 section 1", new Resource(this.getClass().getResourceAsStream("/book2/chapter2_1.html"), "chapter2_1.html"));
		book.addSection("Chapter 3", new Resource(this.getClass().getResourceAsStream("/book2/chapter3.html"), "chapter3.html"));
		return book;
	}
	

	private byte[] writeBookToByteArray(Book book) throws IOException {
		EpubWriter epubWriter = new EpubWriter();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		epubWriter.write(book, out);
		return out.toByteArray();
	}
}
