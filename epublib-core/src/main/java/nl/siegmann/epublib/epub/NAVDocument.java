package nl.siegmann.epublib.epub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.FactoryConfigurationError;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

/**
 * Writes the nav document as defined by namespace http://www.w3.org/1999/xhtml http://purl.org/dc/elements/1.1/ http://www.idpf.org/2007/ops
 * 
 * @author paul
 *
 */
public class NAVDocument {

	public static final String NAMESPACE_XHTML = "http://www.w3.org/1999/xhtml";
	public static final String NAMESPACE_DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";
	public static final String NAMESPACE_EPUB = "http://www.idpf.org/2007/ops";
	public static final String PREFIX_NAV = "nav";
	public static final String NAV_ITEM_ID = "nav";
	public static final String DEFAULT_NAV_HREF = "navigation.html";
	public static final String PREFIX_DTB = "dtb";
	
	private static final Logger log = LoggerFactory.getLogger(NAVDocument.class);

	private interface NAVTags {
		String html = "html";
		String meta = "meta";
		String navPoint = "li";
		String navMap = "ol";
		String navLabel = "span";
		String content = "a";
		String docTitle = "h1";
		String docAuthor = "h2";
		String head = "head";
		String title = "title";
		String body = "body";
		String nav = "nav";
	}
	
	private interface NAVAttributes {
		String name = "name";
		String content = "content";
		String id = "id";
		String clazz = "class";
		String href = "href";
	}

	private interface NAVAttributeValues {

		String chapter = "chapter";

	}
	
	public static Resource read(Book book, EpubReader epubReader) {
		Resource navResource = null;
		if(book.getNavResource() == null) {
			log.error("Book does not contain a table of contents file");
			return navResource;
		}
		try {
			navResource = book.getNavResource();
			if(navResource == null) {
				return navResource;
			}
			Document navDocument = ResourceUtil.getAsDocument(navResource);
			//TODO to search recursively under nav element
			Element navMapElement = DOMUtil.getFirstElementByTagNameNS(navDocument.getDocumentElement(), NAMESPACE_XHTML, NAVTags.navMap);
			TableOfContents tableOfContents = new TableOfContents(readTOCReferences(navMapElement.getChildNodes(), book));
			book.setTableOfContents(tableOfContents);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return navResource;
	}
	
	private static List<TOCReference> readTOCReferences(NodeList navpoints, Book book) {
		if(navpoints == null) {
			return new ArrayList<TOCReference>();
		}
		List<TOCReference> result = new ArrayList<TOCReference>(navpoints.getLength());
		for(int i = 0; i < navpoints.getLength(); i++) {
			Node node = navpoints.item(i);
			if (node.getNodeType() != Document.ELEMENT_NODE) {
				continue;
			}
			if (! (node.getLocalName().equals(NAVTags.navPoint))) {
				continue;
			}
			TOCReference tocReference = readTOCReference((Element) node, book);
			result.add(tocReference);
		}
		return result;
	}

	static TOCReference readTOCReference(Element navpointElement, Book book) {
		String label = readNavLabel(navpointElement);
		String tocResourceRoot = StringUtil.substringBeforeLast(book.getSpine().getTocResource().getHref(), '/');
		if (tocResourceRoot.length() == book.getSpine().getTocResource().getHref().length()) {
			tocResourceRoot = "";
		} else {
			tocResourceRoot = tocResourceRoot + "/";
		}
		String reference = StringUtil.collapsePathDots(tocResourceRoot + readNavReference(navpointElement));
		String href = StringUtil.substringBefore(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
		String fragmentId = StringUtil.substringAfter(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
		Resource resource = book.getResources().getByHref(href);
		if (resource == null) {
			log.error("Resource with href " + href + " in NAV document not found");
		}
		TOCReference result = new TOCReference(label, resource, fragmentId);
		List<TOCReference> childTOCReferences = readTOCReferences(navpointElement.getChildNodes(), book);
		result.setChildren(childTOCReferences);
		return result;
	}
	
	private static String readNavReference(Element navpointElement) {
		Element contentElement = DOMUtil.getFirstElementByTagNameNS(navpointElement, NAMESPACE_XHTML, NAVTags.content);
		String result = DOMUtil.getAttribute(contentElement, NAMESPACE_XHTML, NAVAttributes.href);
		try {
			result = URLDecoder.decode(result, Constants.CHARACTER_ENCODING);
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return result;
	}

	private static String readNavLabel(Element navpointElement) {
		return DOMUtil.getTextChildrenContent(DOMUtil.getFirstElementByTagNameNS(navpointElement, NAMESPACE_XHTML, NAVTags.content));
	}

	
	public static void write(EpubWriter epubWriter, Book book, ZipOutputStream resultStream) throws IOException {
		resultStream.putNextEntry(new ZipEntry(book.getSpine().getTocResource().getHref()));
		XmlSerializer out = EpubProcessorSupport.createXmlSerializer(resultStream);
		write(out, book);
		out.flush();
	}
	

	/**
	 * Generates a resource containing an xml document containing the table of contents of the book in nav format.
	 * 
	 * @param xmlSerializer the serializer used
	 * @param book the book to serialize
	 * 
	 * @throws FactoryConfigurationError
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 */
	public static void write(XmlSerializer xmlSerializer, Book book) throws IllegalArgumentException, IllegalStateException, IOException {
		write(xmlSerializer, book.getMetadata().getIdentifiers(), book.getTitle(), book.getMetadata().getAuthors(), book.getTableOfContents());
	}
	
	public static Resource createNAVResource(Book book) throws IllegalArgumentException, IllegalStateException, IOException {
		return createNAVResource(book.getMetadata().getIdentifiers(), book.getTitle(), book.getMetadata().getAuthors(), book.getTableOfContents());
	}
	public static Resource createNAVResource(List<Identifier> identifiers, String title, List<Author> authors, TableOfContents tableOfContents) throws IllegalArgumentException, IllegalStateException, IOException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		XmlSerializer out = EpubProcessorSupport.createXmlSerializer(data);
		write(out, identifiers, title, authors, tableOfContents);
		Resource resource = new Resource(NAV_ITEM_ID, data.toByteArray(), DEFAULT_NAV_HREF, MediatypeService.XHTML);
		resource.setProperties(NAV_ITEM_ID);
		return resource;
	}	
	
	public static void write(XmlSerializer serializer, List<Identifier> identifiers, String title, List<Author> authors, TableOfContents tableOfContents) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startDocument(Constants.CHARACTER_ENCODING, false);
		serializer.setPrefix(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAMESPACE_XHTML);
		serializer.setPrefix(PackageDocumentWriter.PREFIX_DUBLIN_CORE, PackageDocumentWriter.NAMESPACE_DUBLIN_CORE);
		serializer.setPrefix(PackageDocumentWriter.PREFIX_EPUB, PackageDocumentWriter.NAMESPACE_EPUB);
		serializer.startTag(NAMESPACE_XHTML, NAVTags.html);
		serializer.startTag(NAMESPACE_XHTML, NAVTags.head);

		serializer.startTag(NAMESPACE_XHTML, NAVTags.title);
		serializer.text(StringUtil.defaultIfNull(title));
		serializer.endTag(NAMESPACE_XHTML, NAVTags.title);

		for(Identifier identifier: identifiers) {
			writeMetaElement(identifier.getScheme(), identifier.getValue(), serializer);
		}
		
		writeMetaElement("generator", Constants.EPUBLIB_GENERATOR_NAME, serializer);
		writeMetaElement("depth", String.valueOf(tableOfContents.calculateDepth()), serializer);
		writeMetaElement("totalPageCount", "0", serializer);
		writeMetaElement("maxPageNumber", "0", serializer);

		serializer.endTag(NAMESPACE_XHTML, NAVTags.head);

		serializer.startTag(NAMESPACE_XHTML, NAVTags.body);
		serializer.startTag(NAMESPACE_XHTML, NAVTags.docTitle);
		serializer.text(StringUtil.defaultIfNull(title));
		serializer.endTag(NAMESPACE_XHTML, NAVTags.docTitle);

		for(Author author: authors) {
			serializer.startTag(NAMESPACE_XHTML, NAVTags.docAuthor);
			serializer.text(author.getLastname() + ", " + author.getFirstname());
			serializer.endTag(NAMESPACE_XHTML, NAVTags.docAuthor);
		}

		serializer.startTag(NAMESPACE_XHTML, NAVTags.nav);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, "epub:type", "toc");
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, "id", "toc");
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, "role", "doc-toc");

		writeNavPoints(tableOfContents.getTocReferences(), 1, serializer);

		serializer.endTag(NAMESPACE_XHTML, NAVTags.nav);
		serializer.endTag(NAMESPACE_XHTML, NAVTags.body);

		serializer.endTag(NAMESPACE_XHTML, NAVTags.html);
		serializer.endDocument();
	}


	private static void writeMetaElement(String dtbName, String content, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		serializer.startTag(NAMESPACE_XHTML, NAVTags.meta);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAVAttributes.name, PREFIX_DTB + ":" + dtbName);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAVAttributes.content, content);
		serializer.endTag(NAMESPACE_XHTML, NAVTags.meta);
	}
	
	private static int writeNavPoints(List<TOCReference> tocReferences, int playOrder,
			XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		serializer.startTag(NAMESPACE_XHTML, NAVTags.navMap);
		for(TOCReference tocReference: tocReferences) {
			if (tocReference.getResource() == null) {
				playOrder = writeNavPoints(tocReference.getChildren(), playOrder, serializer);
				continue;
			}
			writeNavPointStart(tocReference, playOrder, serializer);
			playOrder++;
			if(! tocReference.getChildren().isEmpty()) {
				playOrder = writeNavPoints(tocReference.getChildren(), playOrder, serializer);
			}
			writeNavPointEnd(tocReference, serializer);
		}
		serializer.endTag(NAMESPACE_XHTML, NAVTags.navMap);
		return playOrder;
	}


	private static void writeNavPointStart(TOCReference tocReference, int playOrder, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		serializer.startTag(NAMESPACE_XHTML, NAVTags.navPoint);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAVAttributes.id, "navPoint-" + playOrder);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAVAttributes.clazz, NAVAttributeValues.chapter);
		serializer.startTag(NAMESPACE_XHTML, NAVTags.content);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, NAVAttributes.href, tocReference.getCompleteHref());
		serializer.text(tocReference.getTitle());
		serializer.endTag(NAMESPACE_XHTML, NAVTags.content);
	}

	private static void writeNavPointEnd(TOCReference tocReference, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		serializer.endTag(NAMESPACE_XHTML, NAVTags.navPoint);
	}
}
