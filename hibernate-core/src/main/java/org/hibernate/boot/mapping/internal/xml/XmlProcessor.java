/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml;

import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.mapping.internal.xml.XmlProcessingResult.OverrideTuple;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ModelsContext;

import java.util.function.BiConsumer;

import static org.hibernate.boot.mapping.internal.xml.ManagedTypeProcessor.processCompleteEmbeddable;
import static org.hibernate.boot.mapping.internal.xml.ManagedTypeProcessor.processCompleteEntity;
import static org.hibernate.boot.mapping.internal.xml.ManagedTypeProcessor.processCompleteMappedSuperclass;

/**
 * Processes XML mappings - applying metadata-complete mappings and collecting
 * override mappings for later processing.
 *
 * @author Steve Ebersole
 */
public class XmlProcessor {
	public static XmlProcessingResult processXml(
			XmlPreProcessingResult xmlPreProcessingResult,
			PersistenceUnitMetadata persistenceUnitMetadata,
			BiConsumer<JaxbEntityMappingsImpl,XmlDocumentContext> jaxbRootConsumer,
			ModelsContext ModelsContext,
			MetadataBuildingContext metadataBuildingContext,
			RootMappingDefaults mappingDefaults) {
		final boolean xmlMappingsGloballyComplete = persistenceUnitMetadata.areXmlMappingsComplete();
		final var xmlOverlay = new XmlProcessingResultImpl();

		xmlPreProcessingResult.getDocuments().forEach( xmlDocument -> {
			final var xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					mappingDefaults,
					ModelsContext,
					metadataBuildingContext
			);

			final var jaxbRoot = xmlDocument.getRoot();
			jaxbRootConsumer.accept( jaxbRoot, xmlDocumentContext );

			jaxbRoot.getEmbeddables().forEach( jaxbEmbeddable -> {
				if ( xmlMappingsGloballyComplete || jaxbEmbeddable.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					processCompleteEmbeddable( jaxbRoot, jaxbEmbeddable, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEmbeddableOverride(
							new OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEmbeddable ) );
				}
				} );

			jaxbRoot.getMappedSuperclasses().forEach( jaxbMappedSuperclass -> {
				if ( xmlMappingsGloballyComplete || jaxbMappedSuperclass.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					processCompleteMappedSuperclass( jaxbRoot, jaxbMappedSuperclass, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addMappedSuperclassesOverride(
							new OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbMappedSuperclass ) );
				}
				});

			jaxbRoot.getEntities().forEach( jaxbEntity -> {
				if ( xmlMappingsGloballyComplete || jaxbEntity.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					processCompleteEntity( jaxbRoot, jaxbEntity, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEntityOverride(
							new OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEntity ) );
				}
			} );
		} );

		return xmlOverlay;
	}
}
