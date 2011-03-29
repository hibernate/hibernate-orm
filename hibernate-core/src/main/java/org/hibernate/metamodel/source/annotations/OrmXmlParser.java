package org.hibernate.metamodel.source.annotations;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ValidationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.hibernate.metamodel.source.xml.EntityMappings;

/**
 * @author Hardy Ferentschik
 * @todo Need some create some XMLContext as well which can be populated w/ information which can not be expressed via annotations
 */
public class OrmXmlParser {
	private static final Logger log = LoggerFactory.getLogger( OrmXmlParser.class );
	private static final String ORM_MAPPING_XSD = "org/hibernate/ejb/orm_2_0.xsd";

	/**
	 * Parses the given xml configuration files and returns a updated annotation index
	 *
	 * @param mappingFileNames the file names of the xml files to parse
	 * @param annotationIndex the annotation index based on scanned annotations
	 *
	 * @return a new updated annotation index, enhancing and modifying the existing ones according to the jpa xml rules
	 */
	public Index parseAndUpdateIndex(Set<String> mappingFileNames, Index annotationIndex) {

		Set<InputStream> mappingStreams = new HashSet<InputStream>();
		for ( String fileName : mappingFileNames ) {
			mappingStreams.add( getInputStreamForPath( fileName ) );
		}

		for ( InputStream in : mappingStreams ) {
			EntityMappings entityMappings = getEntityMappings( in );
			// ...
		}

		return null;
	}

	private InputStream getInputStreamForPath(String path) {
		// try the context class loader first
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );

		// try the current class loader
		if ( inputStream == null ) {
			inputStream = OrmXmlParser.class.getResourceAsStream( path );
		}
		return inputStream;
	}

	private EntityMappings getEntityMappings(InputStream in) {
		EntityMappings entityMappings;
		Schema schema = getMappingSchema();
		try {
			JAXBContext jc = JAXBContext.newInstance( EntityMappings.class );
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema( schema );
			StreamSource stream = new StreamSource( in );
			JAXBElement<EntityMappings> root = unmarshaller.unmarshal( stream, EntityMappings.class );
			entityMappings = root.getValue();
		}
		catch ( JAXBException e ) {
			String msg = "Error parsing mapping file.";
			log.error( msg );
			throw new ValidationException( msg, e );
		}
		return entityMappings;
	}

	private Schema getMappingSchema() {
		ClassLoader loader = OrmXmlParser.class.getClassLoader();
		URL schemaUrl = loader.getResource( ORM_MAPPING_XSD );
		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		Schema schema = null;
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			log.debug( "Unable to create schema for {}: {}", ORM_MAPPING_XSD, e.getMessage() );
		}
		return schema;
	}
}


