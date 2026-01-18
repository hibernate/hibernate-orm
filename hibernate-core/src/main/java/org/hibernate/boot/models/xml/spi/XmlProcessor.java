/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.spi;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.xml.internal.XmlDocumentContextImpl;
import org.hibernate.boot.models.xml.internal.XmlProcessingResultImpl;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult.OverrideTuple;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.spi.ModelsContext;

import java.util.function.BiConsumer;

import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteEmbeddable;
import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteEntity;
import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteMappedSuperclass;

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
			BootstrapContext bootstrapContext,
			RootMappingDefaults mappingDefaults) {
		final boolean xmlMappingsGloballyComplete = persistenceUnitMetadata.areXmlMappingsComplete();
		final var xmlOverlay = new XmlProcessingResultImpl();

		xmlPreProcessingResult.getDocuments().forEach( xmlDocument -> {
			final var xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					mappingDefaults,
					ModelsContext,
					bootstrapContext
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
