/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListener;
import jakarta.persistence.MappedSuperclass;

/// In-flight holder for various types of "global" registrations.  Also acts as the
/// {@linkplain #createResult builder} for {@linkplain CategorizedDomainModel} as returned
/// by {@linkplain org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer#categorize}
///
/// @since 9.0
/// @author Steve Ebersole
public class DomainModelCategorizationCollector {
	private final boolean areIdGeneratorsGlobal;
	private final Map<String,ClassDetails> sourceClasses = new LinkedHashMap<>();
	private final Set<ClassDetails> sourcePersistentTypes = new LinkedHashSet<>();
	private final Set<ClassDetails> rootEntities = new LinkedHashSet<>();
	private final Map<String,ClassDetails> mappedSuperclasses = new HashMap<>();
	private final Map<String,ClassDetails> embeddables = new HashMap<>();
	private final GlobalRegistrationsImpl globalRegistrations;

	public DomainModelCategorizationCollector(
			boolean areIdGeneratorsGlobal,
			ModelsContext modelsContext,
			Dialect dialect) {
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
		this.globalRegistrations = new GlobalRegistrationsImpl( modelsContext, dialect );
	}

	public Set<ClassDetails> getRootEntities() {
		return rootEntities;
	}

	public Set<ClassDetails> getSourcePersistentTypes() {
		return sourcePersistentTypes;
	}

	public Map<String, ClassDetails> getSourceClasses() {
		return sourceClasses;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	public GlobalRegistrationsImpl getGlobalRegistrations() {
		return globalRegistrations;
	}

	public void applyDiscoveredMappedSuperclass(ClassDetails classDetails) {
		final String className = classDetails.getClassName();
		if ( className != null && !mappedSuperclasses.containsKey( className ) ) {
			collectGlobalRegistrations( classDetails );
			mappedSuperclasses.put( className, classDetails );
		}
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
		getGlobalRegistrations().collectFetchProfiles( jaxbRoot.getFetchProfiles() );
		getGlobalRegistrations().collectImportRenames( jaxbRoot.getHqlImports() );

		final JaxbPersistenceUnitMetadataImpl persistenceUnitMetadata = jaxbRoot.getPersistenceUnitMetadata();
		if ( persistenceUnitMetadata != null ) {
			final JaxbPersistenceUnitDefaultsImpl persistenceUnitDefaults = persistenceUnitMetadata.getPersistenceUnitDefaults();
			if ( persistenceUnitDefaults != null ) {
				final JaxbEntityListenerContainerImpl listenerContainer = persistenceUnitDefaults.getEntityListenerContainer();
				if ( listenerContainer != null ) {
					getGlobalRegistrations().collectEntityListenerRegistrations( listenerContainer.getEntityListeners() );
				}
			}
		}

		getGlobalRegistrations().collectIdGenerators( jaxbRoot );

		getGlobalRegistrations().collectQueryReferences( jaxbRoot, xmlDocumentContext );
		getGlobalRegistrations().collectDataBaseObject( jaxbRoot.getDatabaseObjects() );

		// todo : named graphs
	}

	public void apply(ClassDetails classDetails) {
		sourceClasses.putIfAbsent( classDetails.getName(), classDetails );
		collectGlobalRegistrations( classDetails );

		if ( areIdGeneratorsGlobal ) {
			getGlobalRegistrations().collectIdGenerators( classDetails );
		}

		validateManagedTypeCategory( classDetails );
		if ( classDetails.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
			sourcePersistentTypes.add( classDetails );
			if ( classDetails.getClassName() != null ) {
				mappedSuperclasses.put( classDetails.getClassName(), classDetails );
			}
		}
		else if ( classDetails.hasDirectAnnotationUsage( Entity.class ) ) {
			sourcePersistentTypes.add( classDetails );
			if ( EntityHierarchyBuilder.isRoot( classDetails ) ) {
				rootEntities.add( classDetails );
			}
		}
		else if ( classDetails.hasDirectAnnotationUsage( Embeddable.class ) ) {
			if ( classDetails.getClassName() != null ) {
				embeddables.put( classDetails.getClassName(), classDetails );
			}
		}

		getGlobalRegistrations().collectConverter( classDetails );
	}

	private void collectGlobalRegistrations(ClassDetails classDetails) {
		getGlobalRegistrations().collectJavaTypeRegistrations( classDetails );
		getGlobalRegistrations().collectJdbcTypeRegistrations( classDetails );
		getGlobalRegistrations().collectConverterRegistrations( classDetails );
		getGlobalRegistrations().collectUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCollectionTypeRegistrations( classDetails );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( classDetails );
		getGlobalRegistrations().collectFilterDefinitions( classDetails );
		getGlobalRegistrations().collectFetchProfiles( classDetails );
		getGlobalRegistrations().collectNamedQueryRegistrations( classDetails );
		getGlobalRegistrations().collectSqlResultSetMappingRegistrations( classDetails );
		getGlobalRegistrations().collectNamedEntityGraphRegistrations( classDetails );
		getGlobalRegistrations().collectImportRename( classDetails );
		if ( classDetails.hasDirectAnnotationUsage( EntityListener.class ) ) {
			JpaEventListener.listenerMethods( classDetails ).forEach( getGlobalRegistrations()::addJpaEventListener );
		}
	}

	private static void validateManagedTypeCategory(ClassDetails classDetails) {
		final boolean mappedSuperclass = classDetails.hasDirectAnnotationUsage( MappedSuperclass.class );
		final boolean entity = classDetails.hasDirectAnnotationUsage( Entity.class );
		final boolean embeddable = classDetails.hasDirectAnnotationUsage( Embeddable.class );

		if ( entity && mappedSuperclass ) {
			throw new AnnotationException( "Type '" + classDetails.getName()
					+ "' is annotated both '@Entity' and '@MappedSuperclass'" );
		}
		if ( entity && embeddable ) {
			throw new AnnotationException( "Type '" + classDetails.getName()
					+ "' is annotated both '@Entity' and '@Embeddable'" );
		}
		if ( embeddable && mappedSuperclass ) {
			throw new AnnotationException( "Type '" + classDetails.getName()
					+ "' is annotated both '@Embeddable' and '@MappedSuperclass'" );
		}
	}

	/// Builder for {@linkplain CategorizedDomainModel} based on our internal state plus
	/// the incoming set of managed types.
	///
	/// @param entityHierarchies All entity hierarchies defined in the persistence-unit, built based
	/// on {@linkplain #getRootEntities()}
	///
	/// @see org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer#categorize
	public CategorizedDomainModel createResult(Set<EntityHierarchy> entityHierarchies) {
		return new CategorizedDomainModelImpl(
				entityHierarchies,
				sourceClasses,
				mappedSuperclasses,
				embeddables,
				getGlobalRegistrations()
		);
	}
}
