package org.hibernate.boot.models.xml.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.xml.internal.XmlDocumentContextImpl;
import org.hibernate.boot.models.xml.internal.XmlProcessingResultImpl;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult.OverrideTuple;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteEmbeddable;
import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteEntity;
import static org.hibernate.boot.models.xml.internal.ManagedTypeProcessor.processCompleteMappedSuperclass;
import static org.hibernate.boot.models.xml.internal.XmlProcessingHelper.determineClassName;

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
		final Map<String, JaxbEntityImpl> completeEntitiesByClass = new HashMap<>();

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
					// Complete mappings clear previous metadata. Partial overlays are applied later.
					if ( StringHelper.isNotEmpty( jaxbEntity.getClazz() ) ) {
						final String className = determineClassName( jaxbRoot, jaxbEntity );
						final var previous = completeEntitiesByClass.put( className, jaxbEntity );
						if ( previous != null ) {
							checkCompleteEntityReplacement( className, previous, jaxbEntity, xmlDocument );
						}
					}
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

	private static void checkCompleteEntityReplacement(
			String className,
			JaxbEntityImpl previous,
			JaxbEntityImpl incoming,
			XmlDocument document) {
		final String previousName = effectiveEntityName( className, previous );
		final String incomingName = effectiveEntityName( className, incoming );
		final String removedAttribute = removedBasicAttribute( previous, incoming );
		// A first explicit name may replace the default; an established explicit name may not be lost.
		final boolean losesExplicitName = hasExplicitName( previous ) && !previousName.equals( incomingName );
		if ( losesExplicitName || removedAttribute != null ) {
			throw new MappingException(
					"Conflicting complete XML mappings for entity class '" + className
							+ "': entity names '" + previousName + "' and '" + incomingName + "'"
							+ ( removedAttribute == null ? "" : ", removed basic attribute '" + removedAttribute + "'" ),
					document.getOrigin()
			);
		}
	}

	private static String effectiveEntityName(String className, JaxbEntityImpl entity) {
		return hasExplicitName( entity ) ? entity.getName() : StringHelper.unqualify( className );
	}

	private static boolean hasExplicitName(JaxbEntityImpl entity) {
		final String name = entity.getName();
		return name != null && !name.isBlank();
	}

	private static String removedBasicAttribute(JaxbEntityImpl previous, JaxbEntityImpl incoming) {
		final var previousAttributes = previous.getAttributes();
		final var incomingAttributes = incoming.getAttributes();
		if ( previousAttributes != null && incomingAttributes != null ) {
			for ( var transientAttribute : incomingAttributes.getTransients() ) {
				for ( var previousBasic : previousAttributes.getBasicAttributes() ) {
					if ( transientAttribute.getName().equals( previousBasic.getName() ) ) {
						return previousBasic.getName();
					}
				}
			}
		}
		return null;
	}
}
