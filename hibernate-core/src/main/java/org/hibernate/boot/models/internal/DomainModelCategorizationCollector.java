/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-flight holder for various things as we process metadata sources
 *
 * @author Steve Ebersole
 */
public class DomainModelCategorizationCollector {
	private final GlobalRegistrationsImpl globalRegistrations;
	private final ModelsContext modelsContext;

	private final Set<ClassDetails> rootEntities = new HashSet<>();
	private final Map<String,ClassDetails> mappedSuperclasses = new HashMap<>();
	private final Map<String,ClassDetails> embeddables = new HashMap<>();

	public DomainModelCategorizationCollector(
			GlobalRegistrations globalRegistrations,
			ModelsContext modelsContext) {
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

		getGlobalRegistrations().collectDataBaseObject( jaxbRoot.getDatabaseObjects() );
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
}
