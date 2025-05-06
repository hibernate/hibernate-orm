/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
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
		for ( var mappingXmlBinding : managedResources.getXmlMappingBindings() ) {
			// skip hbm.xml
			if ( mappingXmlBinding.getRoot() instanceof JaxbEntityMappingsImpl ) {
				//noinspection unchecked
				collected.addDocument( (Binding<JaxbEntityMappingsImpl>) mappingXmlBinding );
			}
		}
		return collected;
	}
}
