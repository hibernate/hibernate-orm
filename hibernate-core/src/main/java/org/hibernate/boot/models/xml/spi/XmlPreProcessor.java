/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.spi;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.xml.internal.XmlPreProcessingResultImpl;

/**
 * Performs pre-processing across XML mappings to collect data
 * that makes additional steps easier and more efficient
 *
 * @author Steve Ebersole
 */
public class XmlPreProcessor {

	/**
	 * Build an XmlResources reference based on the given {@code managedResources}
	 */
	public static XmlPreProcessingResult preProcessXmlResources(
			ManagedResources managedResources,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		final XmlPreProcessingResultImpl collected = new XmlPreProcessingResultImpl( persistenceUnitMetadata );

		for ( Binding<JaxbBindableMappingDescriptor> mappingXmlBinding : managedResources.getXmlMappingBindings() ) {
			// for now skip hbm.xml
			final JaxbBindableMappingDescriptor root = mappingXmlBinding.getRoot();
			if ( root instanceof JaxbHbmHibernateMapping ) {
				continue;
			}
			final JaxbEntityMappingsImpl jaxbEntityMappings = (JaxbEntityMappingsImpl) root;
			collected.addDocument( jaxbEntityMappings );
		}

		return collected;
	}
}
