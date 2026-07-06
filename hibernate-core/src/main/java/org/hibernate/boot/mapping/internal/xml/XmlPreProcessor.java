/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;

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
		final var collected = new XmlPreProcessingResultImpl( persistenceUnitMetadata );
		for ( var mappingXmlBinding : managedResources.getXmlMappingBindings() ) {
			collected.addDocument( mappingXmlBinding );
		}
		return collected;
	}

	public static XmlPreProcessingResult preProcessXmlResources(
			PreparedMappingSources resolvedMappingSources,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		final var collected = new XmlPreProcessingResultImpl( persistenceUnitMetadata );
		for ( var mappingXmlBinding : resolvedMappingSources.xmlMappings() ) {
			collected.addDocument( mappingXmlBinding );
		}
		return collected;
	}
}
