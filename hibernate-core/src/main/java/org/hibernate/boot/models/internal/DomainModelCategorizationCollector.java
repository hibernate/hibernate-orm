/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.models.categorize.internal.CategorizedDomainModelImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * In-flight holder for various things as we process metadata sources
 *
 * @author Steve Ebersole
 */

public class DomainModelCategorizationCollector {
	private final boolean areIdGeneratorsGlobal;

	private final GlobalRegistrationsImpl globalRegistrations;
	private final SourceModelBuildingContext modelsContext;

	private final Set<ClassDetails> rootEntities = new HashSet<>();
	private final Map<String,ClassDetails> mappedSuperclasses = new HashMap<>();
	private final Map<String,ClassDetails> embeddables = new HashMap<>();

	public DomainModelCategorizationCollector(
			boolean areIdGeneratorsGlobal,
			GlobalRegistrations globalRegistrations,
			SourceModelBuildingContext modelsContext) {
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
		this.globalRegistrations = (GlobalRegistrationsImpl) globalRegistrations;
		this.modelsContext = modelsContext;
	}

	public GlobalRegistrationsImpl getGlobalRegistrations() {
		return globalRegistrations;
	}

	public Set<ClassDetails> getRootEntities() {
		return rootEntities;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	public void apply(JaxbEntityMappingsImpl jaxbRoot, XmlDocumentContext xmlDocumentContext) {
		getGlobalRegistrations().collectJavaTypeRegistrations( jaxbRoot.getJavaTypeRegistrations() );
		getGlobalRegistrations().collectJdbcTypeRegistrations( jaxbRoot.getJdbcTypeRegistrations() );
		getGlobalRegistrations().collectConverterRegistrations( jaxbRoot.getConverterRegistrations() );
		getGlobalRegistrations().collectConverters( jaxbRoot.getConverters() );
		getGlobalRegistrations().collectUserTypeRegistrations( jaxbRoot.getUserTypeRegistrations() );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( jaxbRoot.getCompositeUserTypeRegistrations() );
		getGlobalRegistrations().collectCollectionTypeRegistrations( jaxbRoot.getCollectionUserTypeRegistrations() );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( jaxbRoot.getEmbeddableInstantiatorRegistrations() );
		getGlobalRegistrations().collectFilterDefinitions( jaxbRoot.getFilterDefinitions() );

		final JaxbPersistenceUnitMetadataImpl persistenceUnitMetadata = jaxbRoot.getPersistenceUnitMetadata();
		if ( persistenceUnitMetadata != null ) {
			final JaxbPersistenceUnitDefaultsImpl persistenceUnitDefaults = persistenceUnitMetadata.getPersistenceUnitDefaults();
			if ( persistenceUnitDefaults != null ) {
				final JaxbEntityListenerContainerImpl listenerContainer = persistenceUnitDefaults.getEntityListenerContainer();
				if ( listenerContainer != null ) {
					getGlobalRegistrations().collectEntityListenerRegistrations( listenerContainer.getEntityListeners(), modelsContext );
				}
			}
		}

		getGlobalRegistrations().collectIdGenerators( jaxbRoot );

		getGlobalRegistrations().collectQueryReferences( jaxbRoot, xmlDocumentContext );

		// todo (7.0) : named graphs?
	}

	public void apply(ClassDetails classDetails) {
		getGlobalRegistrations().collectJavaTypeRegistrations( classDetails );
		getGlobalRegistrations().collectJdbcTypeRegistrations( classDetails );
		getGlobalRegistrations().collectConverterRegistrations( classDetails );
		getGlobalRegistrations().collectUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCollectionTypeRegistrations( classDetails );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( classDetails );
		getGlobalRegistrations().collectFilterDefinitions( classDetails );

		getGlobalRegistrations().collectIdGenerators( classDetails );

		getGlobalRegistrations().collectImportRename( classDetails );

		// todo : named queries
		// todo : named graphs

		if ( classDetails.getDirectAnnotationUsage( MappedSuperclass.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				mappedSuperclasses.put( classDetails.getClassName(), classDetails );
			}
		}
		else if ( classDetails.getDirectAnnotationUsage( Entity.class ) != null ) {
			if ( isRootEntity( classDetails ) ) {
				rootEntities.add( classDetails );
			}
		}
		else if ( classDetails.getDirectAnnotationUsage( Embeddable.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				embeddables.put( classDetails.getClassName(), classDetails );
			}
		}

		if ( ( classDetails.getClassName() != null && classDetails.isImplementor( AttributeConverter.class ) )
				|| classDetails.getDirectAnnotationUsage( Converter.class ) != null ) {
			globalRegistrations.collectConverter( classDetails );
		}
	}

	public static boolean isRootEntity(ClassDetails classInfo) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		if ( classInfo.getSuperClass() == null ) {
			return true;
		}

		ClassDetails current = classInfo.getSuperClass();
		while (  current != null ) {
			if ( current.hasDirectAnnotationUsage( Entity.class ) && !current.isAbstract() ) {
				// a non-abstract super type has `@Entity` -> classInfo cannot be a root entity
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}

	public CategorizedDomainModel createResult(
			Set<EntityHierarchy> entityHierarchies,
			PersistenceUnitMetadata persistenceUnitMetadata,
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		return new CategorizedDomainModelImpl(
				classDetailsRegistry,
				annotationDescriptorRegistry,
				persistenceUnitMetadata,
				entityHierarchies,
				mappedSuperclasses,
				embeddables,
				getGlobalRegistrations()
		);
	}
}
