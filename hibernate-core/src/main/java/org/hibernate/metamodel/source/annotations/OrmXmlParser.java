package org.hibernate.metamodel.source.annotations;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
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

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.xml.EntityMappings;

/**
 * @author Hardy Ferentschik
 * @todo Need some create some XMLContext as well which can be populated w/ information which can not be expressed via annotations
 */
public class OrmXmlParser {
	private static final Logger log = LoggerFactory.getLogger( OrmXmlParser.class );
	private static final String ORM1_MAPPING_XSD = "org/hibernate/ejb/orm_1_0.xsd";
	private static final String ORM2_MAPPING_XSD = "org/hibernate/ejb/orm_2_0.xsd";

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

			Schema schema = getMappingSchema( ORM2_MAPPING_XSD );
			InputStream in = getInputStreamForPath( fileName );
			EntityMappings entityMappings;
			try {
				entityMappings = unmarshallXml( in, schema );
			}
			catch ( JAXBException orm2Exception ) {
				// if we cannot parse against orm_2_0.xsd we try orm_1_0.xsd for backwards compatibility
				try {
					schema = getMappingSchema( ORM1_MAPPING_XSD );
					in = getInputStreamForPath( fileName );
					entityMappings = unmarshallXml( in, schema );
				}
				catch ( JAXBException orm1Exception ) {
					throw new AnnotationException( "Unable to parse xml configuration.", orm1Exception );
				}
			}

			// ..
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

	private EntityMappings unmarshallXml(InputStream in, Schema schema) throws JAXBException {
		EntityMappings entityMappings;
		JAXBContext jc = JAXBContext.newInstance( EntityMappings.class );
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		unmarshaller.setSchema( schema );
		StreamSource stream = new StreamSource( in );
		JAXBElement<EntityMappings> root = unmarshaller.unmarshal( stream, EntityMappings.class );
		entityMappings = root.getValue();
		return entityMappings;
	}

	private Schema getMappingSchema(String schemaVersion) {
		ClassLoader loader = OrmXmlParser.class.getClassLoader();
		URL schemaUrl = loader.getResource( schemaVersion );
		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		Schema schema = null;
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			log.debug( "Unable to create schema for {}: {}", ORM2_MAPPING_XSD, e.getMessage() );
		}
		return schema;
	}
}


