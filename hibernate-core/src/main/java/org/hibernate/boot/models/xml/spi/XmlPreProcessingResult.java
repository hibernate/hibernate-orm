/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.spi;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;

/**
 * Result of {@linkplain XmlPreProcessor#preProcessXmlResources}
 *
 * @author Steve Ebersole
 */
public interface XmlPreProcessingResult {
	/**
	 * Aggregated persistence unit defaults and metadata
	 */
	PersistenceUnitMetadata getPersistenceUnitMetadata();

	/**
	 * All XML documents (JAXB roots)
	 */
	List<JaxbEntityMappingsImpl> getDocuments();

	/**
	 * All classes named across all XML mappings
	 *
	 * @see JaxbManagedType#getClazz()
	 */
	List<String> getMappedClasses();

	/**
	 * All "type names" named across all XML mappings.
	 *
	 * @apiNote This accounts for dynamic models
	 *
	 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl#getName()
	 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl#getName()
	 */
	List<String> getMappedNames();
}
