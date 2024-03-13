/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.IndexView;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * In-flight holder for various types of "global" registrations.  Also acts as the
 * {@linkplain #createResult builder} for {@linkplain CategorizedDomainModel} as returned
 * by {@linkplain ManagedResourcesProcessor#processManagedResources}
 *
 * @author Steve Ebersole
 */

public class DomainModelCategorizationCollector {
	private final boolean areIdGeneratorsGlobal;
	private final IndexView jandexIndex;

	private final GlobalRegistrationsImpl globalRegistrations;
	private final SourceModelBuildingContext modelsContext;

	private final Set<ClassDetails> rootEntities = new HashSet<>();
	private final Map<String,ClassDetails> mappedSuperclasses = new HashMap<>();
	private final Map<String,ClassDetails> embeddables = new HashMap<>();

	public DomainModelCategorizationCollector(
			boolean areIdGeneratorsGlobal,
			GlobalRegistrations globalRegistrations,
			IndexView jandexIndex,
			SourceModelBuildingContext modelsContext) {
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
		this.jandexIndex = jandexIndex;
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

	public void apply(JaxbEntityMappingsImpl jaxbRoot) {
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

		getGlobalRegistrations().collectQueryReferences( jaxbRoot );

		// todo : named graphs
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

		if ( areIdGeneratorsGlobal ) {
			getGlobalRegistrations().collectIdGenerators( classDetails );
		}

		getGlobalRegistrations().collectImportRename( classDetails );

		// todo : named queries
		// todo : named graphs

		if ( classDetails.getAnnotationUsage( MappedSuperclass.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				mappedSuperclasses.put( classDetails.getClassName(), classDetails );
			}
		}
		else if ( classDetails.getAnnotationUsage( Entity.class ) != null ) {
			if ( EntityHierarchyBuilder.isRoot( classDetails ) ) {
				rootEntities.add( classDetails );
			}
		}
		else if ( classDetails.getAnnotationUsage( Embeddable.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				embeddables.put( classDetails.getClassName(), classDetails );
			}
		}

		if ( ( classDetails.getClassName() != null && classDetails.isImplementor( AttributeConverter.class ) )
				|| classDetails.getAnnotationUsage( Converter.class ) != null ) {
			globalRegistrations.collectConverter( classDetails );
		}
	}

	/**
	 * Builder for {@linkplain CategorizedDomainModel} based on our internal state plus
	 * the incoming set of managed types.
	 *
	 * @param entityHierarchies All entity hierarchies defined in the persistence-unit, built based
	 * on {@linkplain #getRootEntities()}
	 *
	 * @see ManagedResourcesProcessor#processManagedResources
	 */
	public CategorizedDomainModel createResult(
			Set<EntityHierarchy> entityHierarchies,
			PersistenceUnitMetadata persistenceUnitMetadata,
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		return new CategorizedDomainModelImpl(
				classDetailsRegistry,
				annotationDescriptorRegistry,
				jandexIndex,
				persistenceUnitMetadata,
				entityHierarchies,
				mappedSuperclasses,
				embeddables,
				getGlobalRegistrations()
		);
	}
}
