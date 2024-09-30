package nl.siegmann.epublib.epub;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.util.CollectionUtil;

public class EpubWriterTest3 {

	@Test
	public void testBook3() throws IOException {
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
		Assert.assertEquals(CollectionUtil.first(book.getMetadata().getIdentifiers()).getValue(), CollectionUtil.first(readBook.getMetadata().getIdentifiers()).getValue());
		Assert.assertEquals(CollectionUtil.first(book.getMetadata().getAuthors()), CollectionUtil.first(readBook.getMetadata().getAuthors()));
		Assert.assertEquals(2, readBook.getSpine().size());
		Assert.assertEquals(1, readBook.getTableOfContents().size());
			
	}
	
	private Book createTestBook() throws IOException {
		Book book = new Book();
		
		book.getMetadata().addTitle("Epublib test book 1");
		book.getMetadata().addTitle("test3");
		
		book.getMetadata().addIdentifier(new Identifier(Identifier.Scheme.ISBN, "987654321"));
		book.getMetadata().addAuthor(new Author("Joe", "Tester"));
		book.addSection("Chapter 1", new Resource(this.getClass().getResourceAsStream("/book3/一.xhtml"), "xhtml/一.xhtml"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book3/一.smil"), "xhtml/一.smil"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book3/horizontal.css"), "css/horizontal.css"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book3/vertical.css"), "css/vertical.css"));
		book.addResource(new Resource(this.getClass().getResourceAsStream("/book3/fmse004b.mp3"), "audio/fmse004b.mp3"));
		return book;
	}
	

	private byte[] writeBookToByteArray(Book book) throws IOException {
		EpubWriter epubWriter = new EpubWriter();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		epubWriter.write(book, out);
		return out.toByteArray();
	}
}
