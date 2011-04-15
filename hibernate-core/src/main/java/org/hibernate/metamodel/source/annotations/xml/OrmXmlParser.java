package org.hibernate.metamodel.source.annotations.xml;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBException;

import org.jboss.jandex.Index;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.annotation.xml.EntityMappings;
import org.hibernate.metamodel.source.util.xml.XmlHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Hardy Ferentschik
 * @todo Need some create some XMLContext as well which can be populated w/ information which can not be expressed via annotations
 */
public class OrmXmlParser {
	private static final String ORM1_MAPPING_XSD = "org/hibernate/ejb/orm_1_0.xsd";
	private static final String ORM2_MAPPING_XSD = "org/hibernate/ejb/orm_2_0.xsd";

	private final MetadataImpl meta;

	public OrmXmlParser(MetadataImpl meta) {
		this.meta = meta;
	}

	/**
	 * Parses the given xml configuration files and returns a updated annotation index
	 *
	 * @param mappingFileNames the file names of the xml files to parse
	 * @param annotationIndex the annotation index based on scanned annotations
	 *
	 * @return a new updated annotation index, enhancing and modifying the existing ones according to the jpa xml rules
	 */
	public Index parseAndUpdateIndex(Set<String> mappingFileNames, Index annotationIndex) {
		ClassLoaderService classLoaderService = meta.getServiceRegistry().getService( ClassLoaderService.class );
		Set<InputStream> mappingStreams = new HashSet<InputStream>();
		for ( String fileName : mappingFileNames ) {

			EntityMappings entityMappings;
			try {
				entityMappings = XmlHelper.unmarshallXml(
						fileName, ORM2_MAPPING_XSD, EntityMappings.class, classLoaderService
				).getRoot();
			}
			catch ( JAXBException orm2Exception ) {
				// if we cannot parse against orm_2_0.xsd we try orm_1_0.xsd for backwards compatibility
				try {
					entityMappings = XmlHelper.unmarshallXml(
							fileName, ORM1_MAPPING_XSD, EntityMappings.class, classLoaderService
					).getRoot();
				}
				catch ( JAXBException orm1Exception ) {
					throw new AnnotationException( "Unable to parse xml configuration.", orm1Exception );
				}
			}

			entityMappings.toString();
		}

		return null;
	}
}


