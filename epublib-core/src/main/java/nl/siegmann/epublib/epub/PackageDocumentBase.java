package nl.siegmann.epublib.epub;


/**
 * Functionality shared by the PackageDocumentReader and the PackageDocumentWriter
 *  
 * @author paul
 *
 */
public class PackageDocumentBase {
	public static final String BOOK_ID_ID = "BookId";
	public static final String NAMESPACE_OPF = "http://www.idpf.org/2007/opf";
	public static final String NAMESPACE_DUBLIN_CORE = "http://purl.org/dc/elements/1.1/";
	public static final String NAMESPACE_EPUB = "http://www.idpf.org/2007/ops";
	public static final String NAMESPACE_XHTML = "http://www.w3.org/1999/xhtml";
	public static final String NAMESPACE_SMIL = "http://www.w3.org/ns/SMIL";
	public static final String PREFIX_DUBLIN_CORE = "dc";
	public static final String PREFIX_OPF = "opf";
	public static final String PREFIX_EPUB = "epub";
	public static final String dateFormat = "yyyy-MM-dd";
	public static final String EPUB_VERSION = "3.0";

	protected interface DCTags {
		String title = "title";
        String creator = "creator";
        String subject = "subject";
        String description = "description";
        String publisher = "publisher";
        String contributor = "contributor";
        String date = "date";
        String type = "type";
        String format = "format";
        String identifier = "identifier";
        String source = "source";
        String language = "language";
        String relation = "relation";
        String coverage = "coverage";
        String rights = "rights";
	}
	
	protected interface DCAttributes {
		String scheme = "scheme";
		String id = "id";
	}
	
	protected interface OPFTags {
		String metadata = "metadata";
//20240928
		String meta = "meta";
		String manifest = "manifest";
		String packageTag = "package";
		String itemref = "itemref";
		String spine = "spine";
		String reference = "reference";
		String guide = "guide";
		String item = "item";
	}
	
	protected interface OPFAttributes {
		String uniqueIdentifier = "unique-identifier";
		String idref = "idref";
		String name = "name";
		String content = "content";
		String type = "type";
		String href = "href";
		String linear = "linear";
		String event = "event";
		String role = "role";
		String file_as = "file-as";
		String id = "id";
		String media_type = "media-type";
		String title = "title";
		String toc = "toc";
		String version = "version";
		String scheme = "scheme";
		String property = "property";
		String properties = "properties";
		String mediaOverlay = "media-overlay";
	}
	
	protected interface OPFValues {
		String meta_cover = "cover";
		String reference_cover = "cover";
		String no = "no";
		String generator = "generator";
//20240928
		String modified = "dcterms:modified";
	}
}