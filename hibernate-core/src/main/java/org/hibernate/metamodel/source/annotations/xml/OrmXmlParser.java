package org.hibernate.metamodel.source.annotations.xml;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;

import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.xml.mocker.EntityMappingsMocker;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.internal.MetadataImpl;

//import org.hibernate.metamodel.source.util.xml.XmlHelper;

/**
 * @author Hardy Ferentschik
 * @todo Need some create some XMLContext as well which can be populated w/ information which can not be expressed via annotations
 */
public class OrmXmlParser {
	private final MetadataImpl meta;

	public OrmXmlParser(MetadataImpl meta) {
		this.meta = meta;
	}

	/**
	 * Parses the given xml configuration files and returns a updated annotation index
	 *
	 * @param mappings list of {@code XMLEntityMappings} created from the specified orm xml files
	 * @param annotationIndex the annotation index based on scanned annotations
	 *
	 * @return a new updated annotation index, enhancing and modifying the existing ones according to the jpa xml rules
	 */
	public Index parseAndUpdateIndex(List<JaxbRoot<XMLEntityMappings>> mappings, Index annotationIndex) {
		List<XMLEntityMappings> list = new ArrayList<XMLEntityMappings>( mappings.size() );
		for ( JaxbRoot<XMLEntityMappings> jaxbRoot : mappings ) {
			list.add( jaxbRoot.getRoot() );
		}
		return new EntityMappingsMocker(
				list, annotationIndex, meta.getServiceRegistry()
		).mockNewIndex();
	}



}


