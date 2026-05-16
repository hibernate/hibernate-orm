/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hibernate.boot.model.internal.EmbeddableBinder.isEmbeddable;
import static org.hibernate.boot.model.internal.EntityBinder.isEntity;
import static org.hibernate.boot.model.internal.EntityBinder.isMappedSuperclass;

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
	private final Set<String> idClasses = new HashSet<>();
	private final Set<ClassDetails> entityListenerClasses = new HashSet<>();
	private final Set<String> packageNames = new HashSet<>();

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

	public Set<String> getIdClasses() {
		return idClasses;
	}

	/**
	 * Classes that have methods annotated with JPA lifecycle callback annotations
	 * ({@code @PrePersist}, {@code @PostPersist}, {@code @PreRemove}, {@code @PostRemove},
	 * {@code @PreUpdate}, {@code @PostUpdate}, {@code @PostLoad}).
	 * <p>
	 * These are classes that act as entity listeners (either standalone listener classes
	 * referenced via {@code @EntityListeners} or entity classes with callback methods).
	 */
	public Set<ClassDetails> getEntityListenerClasses() {
		return entityListenerClasses;
	}

	/**
	 * Package names discovered from {@code package-info} classes encountered
	 * during categorization.
	 */
	public Set<String> getPackageNames() {
		return packageNames;
	}

	public void apply(JaxbEntityMappingsImpl jaxbRoot, XmlDocumentContext xmlDocumentContext) {
		globalRegistrations.collectJavaTypeRegistrations( jaxbRoot.getJavaTypeRegistrations() );
		globalRegistrations.collectJdbcTypeRegistrations( jaxbRoot.getJdbcTypeRegistrations() );
		globalRegistrations.collectConverterRegistrations( jaxbRoot.getConverterRegistrations() );
		globalRegistrations.collectConverters( jaxbRoot.getConverters() );
		globalRegistrations.collectUserTypeRegistrations( jaxbRoot.getUserTypeRegistrations() );
		globalRegistrations.collectCompositeUserTypeRegistrations( jaxbRoot.getCompositeUserTypeRegistrations() );
		globalRegistrations.collectCollectionTypeRegistrations( jaxbRoot.getCollectionUserTypeRegistrations() );
		globalRegistrations.collectEmbeddableInstantiatorRegistrations( jaxbRoot.getEmbeddableInstantiatorRegistrations() );
		globalRegistrations.collectFilterDefinitions( jaxbRoot.getFilterDefinitions() );

		final var persistenceUnitMetadata = jaxbRoot.getPersistenceUnitMetadata();
		if ( persistenceUnitMetadata != null ) {
			final var persistenceUnitDefaults = persistenceUnitMetadata.getPersistenceUnitDefaults();
			if ( persistenceUnitDefaults != null ) {
				final var listenerContainer = persistenceUnitDefaults.getEntityListenerContainer();
				if ( listenerContainer != null ) {
					getGlobalRegistrations()
							.collectEntityListenerRegistrations( listenerContainer.getEntityListeners(), modelsContext );
				}
			}
		}

		getGlobalRegistrations().collectIdGenerators( jaxbRoot );

		getGlobalRegistrations().collectQueryReferences( jaxbRoot, xmlDocumentContext );

		getGlobalRegistrations().collectDataBaseObject( jaxbRoot.getDatabaseObjects() );
		// todo (7.0) : named graphs?
	}

	public void apply(ClassDetails classDetails) {
		globalRegistrations.collectJavaTypeRegistrations( classDetails );
		globalRegistrations.collectJdbcTypeRegistrations( classDetails );
		globalRegistrations.collectConverterRegistrations( classDetails );
		globalRegistrations.collectUserTypeRegistrations( classDetails );
		globalRegistrations.collectCompositeUserTypeRegistrations( classDetails );
		globalRegistrations.collectCollectionTypeRegistrations( classDetails );
		globalRegistrations.collectEmbeddableInstantiatorRegistrations( classDetails );
		globalRegistrations.collectFilterDefinitions( classDetails );

		globalRegistrations.collectIdGenerators( classDetails );

		globalRegistrations.collectImportRename( classDetails );

		// todo : named queries
		// todo : named graphs

		if ( isMappedSuperclass( classDetails ) ) {
			if ( classDetails.getClassName() != null ) {
				mappedSuperclasses.put( classDetails.getClassName(), classDetails );
			}
		}
		else if ( isEntity( classDetails ) ) {
			if ( isRootEntity( classDetails ) ) {
				rootEntities.add( classDetails );
			}
		}
		else if ( isEmbeddable( classDetails ) ) {
			if ( classDetails.getClassName() != null ) {
				embeddables.put( classDetails.getClassName(), classDetails );
			}
		}

		if ( hasIdClass( classDetails ) ) {
			idClasses.add( classDetails.getDirectAnnotationUsage( IdClass.class ).value().getName() );
		}

		if ( isConverter( classDetails ) ) {
			globalRegistrations.collectConverter( classDetails );
		}

		if ( hasJpaLifecycleCallbackMethods( classDetails ) ) {
			entityListenerClasses.add( classDetails );
		}

		if ( isPackageInfo( classDetails ) ) {
			String className = classDetails.getClassName();
			packageNames.add( className.substring( 0, className.lastIndexOf( '.' ) ) );
		}
	}

	private static boolean hasJpaLifecycleCallbackMethods(ClassDetails classDetails) {
		return classDetails.getMethods().stream().anyMatch( method ->
				method.hasDirectAnnotationUsage( PrePersist.class )
				|| method.hasDirectAnnotationUsage( PostPersist.class )
				|| method.hasDirectAnnotationUsage( PreRemove.class )
				|| method.hasDirectAnnotationUsage( PostRemove.class )
				|| method.hasDirectAnnotationUsage( PreUpdate.class )
				|| method.hasDirectAnnotationUsage( PostUpdate.class )
				|| method.hasDirectAnnotationUsage( PostLoad.class ) );
	}

	private static boolean hasIdClass(ClassDetails classDetails) {
		return classDetails.getDirectAnnotationUsage( IdClass.class ) != null;
	}

	private static boolean isPackageInfo(ClassDetails classDetails) {
		String className = classDetails.getClassName();
		return className != null && className.endsWith( ".package-info" );
	}

	private static boolean isConverter(ClassDetails classDetails) {
		return classDetails.getClassName() != null && classDetails.isImplementor( AttributeConverter.class )
			|| classDetails.getDirectAnnotationUsage( Converter.class ) != null;
	}

	public static boolean isRootEntity(ClassDetails classInfo) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		var current = classInfo.getSuperClass();
		while ( current != null ) {
			if ( isEntity( current ) && !current.isAbstract() ) {
				// a non-abstract super type has `@Entity` -> classInfo cannot be a root entity
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}
}
