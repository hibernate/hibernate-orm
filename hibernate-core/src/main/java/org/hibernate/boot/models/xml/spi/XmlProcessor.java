/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.spi;

import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.xml.internal.ManagedTypeProcessor;
import org.hibernate.boot.models.xml.internal.XmlDocumentContextImpl;
import org.hibernate.boot.models.xml.internal.XmlDocumentImpl;
import org.hibernate.boot.models.xml.internal.XmlProcessingResultImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.spi.SourceModelBuildingContext;

/**
 * Processes XML mappings - applying metadata-complete mappings and collecting
 * override mappings for later processing.
 *
 * @author Steve Ebersole
 */
public class XmlProcessor {
	public static XmlProcessingResult processXml(
			XmlPreProcessingResult xmlPreProcessingResult,
			DomainModelCategorizationCollector modelCategorizationCollector,
			SourceModelBuildingContext sourceModelBuildingContext,
			BootstrapContext bootstrapContext) {
		final boolean xmlMappingsGloballyComplete = xmlPreProcessingResult.getPersistenceUnitMetadata().areXmlMappingsComplete();
		final XmlProcessingResultImpl xmlOverlay = new XmlProcessingResultImpl();

		xmlPreProcessingResult.getDocuments().forEach( (jaxbRoot) -> {
			modelCategorizationCollector.apply( jaxbRoot );
			final XmlDocumentImpl xmlDocument = XmlDocumentImpl.consume(
					jaxbRoot,
					xmlPreProcessingResult.getPersistenceUnitMetadata()
			);
			final XmlDocumentContext xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					xmlPreProcessingResult.getPersistenceUnitMetadata(),
					sourceModelBuildingContext,
					bootstrapContext
			);

			jaxbRoot.getEmbeddables().forEach( (jaxbEmbeddable) -> {
				if ( xmlMappingsGloballyComplete || jaxbEmbeddable.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEmbeddable(
							jaxbRoot,
							jaxbEmbeddable,
							xmlDocumentContext
					);
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEmbeddableOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEmbeddable ) );
				}
			} );

			jaxbRoot.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
				if ( xmlMappingsGloballyComplete || jaxbMappedSuperclass.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteMappedSuperclass( jaxbRoot, jaxbMappedSuperclass, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addMappedSuperclassesOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbMappedSuperclass ) );
				}
			});

			jaxbRoot.getEntities().forEach( (jaxbEntity) -> {
				if ( xmlMappingsGloballyComplete || jaxbEntity.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEntity( jaxbRoot, jaxbEntity, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEntityOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEntity ) );
				}
			} );
		} );

		return xmlOverlay;
	}
}
