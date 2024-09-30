package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Guide;
import nl.siegmann.epublib.domain.GuideReference;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;


/**
 * Writes the opf package document as defined by namespace http://www.idpf.org/2007/opf
 *  
 * @author paul
 *
 */
public class PackageDocumentWriter extends PackageDocumentBase {

	private static final Logger log = LoggerFactory.getLogger(PackageDocumentWriter.class);

	public static void write(EpubWriter epubWriter, XmlSerializer serializer, Book book) throws IOException {
		try {
			serializer.startDocument(Constants.CHARACTER_ENCODING, false);
			serializer.setPrefix(PREFIX_OPF, NAMESPACE_OPF);
			serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);
			serializer.startTag(NAMESPACE_OPF, OPFTags.packageTag);
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.version, EPUB_VERSION);
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.uniqueIdentifier, BOOK_ID_ID);

			PackageDocumentMetadataWriter.writeMetaData(book, serializer);
			
			writeManifest(book, epubWriter, serializer);
			writeSpine(book, epubWriter, serializer);
//20240928
//			writeGuide(book, epubWriter, serializer);
			
			serializer.endTag(NAMESPACE_OPF, OPFTags.packageTag);
			serializer.endDocument();
			serializer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Writes the package's spine.
	 * 
	 * @param book
	 * @param epubWriter
	 * @param serializer
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 * @throws XMLStreamException
	 */
	private static void writeSpine(Book book, EpubWriter epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(NAMESPACE_OPF, OPFTags.spine);
//20240928
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.toc, book.getSpine().getTocResource().getId());
		serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, book.getSpine().getTocResource().getId());
		serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);

		if(book.getCoverPage() != null // there is a cover page
			&&	book.getSpine().findFirstResourceById(book.getCoverPage().getId()) < 0) { // cover page is not already in the spine
			// write the cover html file
			serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, book.getCoverPage().getId());
//TODO 20240928
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, "no");
			serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
		}
		writeSpineItems(book.getSpine(), serializer);
		serializer.endTag(NAMESPACE_OPF, OPFTags.spine);
	}

	
	private static void writeManifest(Book book, EpubWriter epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(NAMESPACE_OPF, OPFTags.manifest);

//		serializer.startTag(NAMESPACE_OPF, OPFTags.item);
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, epubWriter.getNavId());
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, epubWriter.getNavHref());
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.media_type, epubWriter.getNavMediaType());
//20240928
//		if (epubWriter.getNavId().equals(NAVDocument.NAV_ITEM_ID)) {
//			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.properties, NAVDocument.NAV_ITEM_ID);
//		}
//		serializer.endTag(NAMESPACE_OPF, OPFTags.item);

//		writeCoverResources(book, serializer);

		setMediaOverlayReference(book, book.getResources());

		for(Resource resource: getAllResourcesSortById(book)) {
			writeItem(book, resource, serializer);
		}
		
		serializer.endTag(NAMESPACE_OPF, OPFTags.manifest);
	}

	private static List<Resource> getAllResourcesSortById(Book book) {
		List<Resource> allResources = new ArrayList<Resource>(book.getResources().getAll());
		Collections.sort(allResources, new Comparator<Resource>() {

			@Override
			public int compare(Resource resource1, Resource resource2) {
				return resource1.getId().compareToIgnoreCase(resource2.getId());
			}
		});
		return allResources;
	}
	
	/**
	 * Writes a resources as an item element
	 * @param resource
	 * @param serializer
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 * @throws XMLStreamException
	 */
	private static void writeItem(Book book, Resource resource, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		if(resource == null ||
				(resource.getMediaType() == MediatypeService.NCX
				&& book.getSpine().getTocResource() != null)) {
			return;
		}
		if(StringUtil.isBlank(resource.getId())) {
			log.error("resource id must not be empty (href: " + resource.getHref() + ", mediatype:" + resource.getMediaType() + ")");
			return;
		}
		if(StringUtil.isBlank(resource.getHref())) {
			log.error("resource href must not be empty (id: " + resource.getId() + ", mediatype:" + resource.getMediaType() + ")");
			return;
		}
		if(resource.getMediaType() == null) {
			log.error("resource mediatype must not be empty (id: " + resource.getId() + ", href:" + resource.getHref() + ")");
			return;
		}
		serializer.startTag(NAMESPACE_OPF, OPFTags.item);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, resource.getId());
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, resource.getHref());
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.media_type, resource.getMediaType().getName());
		if (resource.getId().equals(NAVDocument.NAV_ITEM_ID)) {
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.properties, NAVDocument.NAV_ITEM_ID);
		}
		if (StringUtil.isNotBlank(resource.getMeidaOverlay())) {
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.mediaOverlay, resource.getMeidaOverlay());
		}
		serializer.endTag(NAMESPACE_OPF, OPFTags.item);
	}

	/**
	 * List all spine references
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 */
	private static void writeSpineItems(Spine spine, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		for(SpineReference spineReference: spine.getSpineReferences()) {
			serializer.startTag(NAMESPACE_OPF, OPFTags.itemref);
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, spineReference.getResourceId());
			if (! spineReference.isLinear()) {
				serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, OPFValues.no);
			}
			serializer.endTag(NAMESPACE_OPF, OPFTags.itemref);
		}
	}

	private static void writeGuide(Book book, EpubWriter epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(NAMESPACE_OPF, OPFTags.guide);
		ensureCoverPageGuideReferenceWritten(book.getGuide(), epubWriter, serializer);
		for (GuideReference reference: book.getGuide().getReferences()) {
			writeGuideReference(reference, serializer);
		}
		serializer.endTag(NAMESPACE_OPF, OPFTags.guide);
	}
	
	private static void ensureCoverPageGuideReferenceWritten(Guide guide,
			EpubWriter epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		if (! (guide.getGuideReferencesByType(GuideReference.COVER).isEmpty())) {
			return;
		}
		Resource coverPage = guide.getCoverPage();
		if (coverPage != null) {
			writeGuideReference(new GuideReference(guide.getCoverPage(), GuideReference.COVER, GuideReference.COVER), serializer);
		}
	}


	private static void writeGuideReference(GuideReference reference, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		if (reference == null) {
			return;
		}
		serializer.startTag(NAMESPACE_OPF, OPFTags.reference);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.type, reference.getType());
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, reference.getCompleteHref());
		if (StringUtil.isNotBlank(reference.getTitle())) {
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.title, reference.getTitle());
		}
		serializer.endTag(NAMESPACE_OPF, OPFTags.reference);
	}

	private static void setMediaOverlayReference(Book book, Resources resources) {
		for (Resource resource : getAllResourcesSortById(book)) {
			if (resource.getMediaType() == MediatypeService.SMIL) {
				String href = getMediaOverlayReferenceUrl(resource);
				Resource refResource = resources.getByIdOrHref(href);
				if (refResource != null) {
					refResource.setMediaOverlay(resource.getId());
				}
				href = StringUtil.substringBeforeLast(resource.getHref(), '/') + "/" + href;
				refResource = resources.getByIdOrHref(href);
				if (refResource != null) {
					refResource.setMediaOverlay(resource.getId());
				}
			}
		}
	}

	private static String getMediaOverlayReferenceUrl(Resource resource) {
		if (resource.getMediaType() == MediatypeService.SMIL) {
			try {
				Document document = ResourceUtil.getAsDocument(resource);
				NodeList metaTags = document.getElementsByTagNameNS(NAMESPACE_SMIL, "text");
				for (int i = 0; i < metaTags.getLength(); i++) {
					Element metaElement = (Element) metaTags.item(i);
					String href = metaElement.getAttribute("src");
					if (StringUtil.isNotBlank(href)) {
						href = StringUtil.substringBefore(href, Constants.FRAGMENT_SEPARATOR_CHAR);
						return href;
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
}