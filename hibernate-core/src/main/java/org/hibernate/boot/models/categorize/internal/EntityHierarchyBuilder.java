/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Builds {@link EntityHierarchy} references from
 * {@linkplain ClassDetailsRegistry#forEachClassDetails managed classes}.
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyBuilder {

	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param typeConsumer Callback for any identifiable-type metadata references
	 * @param buildingContext The table context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public static Set<EntityHierarchy> createEntityHierarchies(
			Set<ClassDetails> rootEntities,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext buildingContext) {
		return new EntityHierarchyBuilder( buildingContext ).process( rootEntities, typeConsumer );
	}

	/**
	 * Pre-processes the annotated entities from the index and create a set of entity hierarchies which can be bound
	 * to the metamodel.
	 *
	 * @param typeConsumer Callback for any identifiable-type metadata references
	 * @param buildingContext The table context, giving access to needed services and information
	 *
	 * @return a set of {@code EntityHierarchySource} instances.
	 */
	public static Set<EntityHierarchy> createEntityHierarchies(
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext buildingContext) {
		return createEntityHierarchies(
				collectRootEntityTypes( buildingContext.getClassDetailsRegistry() ),
				typeConsumer,
				buildingContext
		);
	}

	private final ModelCategorizationContext modelContext;

	public EntityHierarchyBuilder(ModelCategorizationContext modelContext) {
		this.modelContext = modelContext;
	}

	private Set<EntityHierarchy> process(
			Set<ClassDetails> rootEntities,
			HierarchyTypeConsumer typeConsumer) {
		final Set<EntityHierarchy> hierarchies = CollectionHelper.setOfSize( rootEntities.size() );

		rootEntities.forEach( (rootEntity) -> {
			final AccessType defaultAccessType = determineDefaultAccessTypeForHierarchy( rootEntity );
			hierarchies.add( new EntityHierarchyImpl(
					rootEntity,
					defaultAccessType,
					org.hibernate.cache.spi.access.AccessType.TRANSACTIONAL,
					typeConsumer,
					modelContext
			) );
		} );

		return hierarchies;
	}

	private AccessType determineDefaultAccessTypeForHierarchy(ClassDetails rootEntityType) {
		assert rootEntityType != null;

//		// look for `@Access` at class level
//		final AccessType classAnnotationValue = resolveDefaultAccessTypeFromClassAnnotation( rootEntityType );
//		if ( classAnnotationValue != null ) {
//			return classAnnotationValue;
//		}

		// look for `@Id` or `@EmbeddedId`
		// todo (jpa32) : technically we could probably look for member with any "mapping" annotation
		final AccessType accessFromAttribute = resolveDefaultAccessTypeFromMembers( rootEntityType );
		if ( accessFromAttribute != null ) {
			return accessFromAttribute;
		}


//		// 2.3.1 Default Access Type
//		//    It is an error if a default access type cannot be determined and an access type is not explicitly specified
//		//    by means of annotations or the XML descriptor.
//		throw new AccessTypeDeterminationException( rootEntityType );

		return null;
	}

	private AccessType resolveDefaultAccessTypeFromClassAnnotation(ClassDetails rootEntityType) {
		ClassDetails current = rootEntityType;
		while ( current != null ) {
			final Access accessAnnotation = current.getDirectAnnotationUsage( Access.class );
			if ( accessAnnotation != null ) {
				return accessAnnotation.value();
			}

			current = current.getSuperClass();
		}

		return null;
	}

	private AccessType resolveDefaultAccessTypeFromMembers(ClassDetails rootEntityType) {
		ClassDetails current = rootEntityType;
		while ( current != null ) {
			// look for `@Id` or `@EmbeddedId` (w/o `@Access`)
			final AnnotationTarget idMember = determineIdMember( current );
			if ( idMember != null ) {
				switch ( idMember.getKind() ) {
					case FIELD: {
						return AccessType.FIELD;
					}
					case METHOD: {
						return AccessType.PROPERTY;
					}
					default: {
						throw new IllegalStateException( "@Id / @EmbeddedId found on target other than field or method : " + idMember );
					}
				}
			}

			current = current.getSuperClass();
		}

		return null;
	}

	private AnnotationTarget determineIdMember(ClassDetails current) {
		final List<MethodDetails> methods = current.getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			if ( methodDetails.hasDirectAnnotationUsage( Id.class )
					|| methodDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				if ( methodDetails.getDirectAnnotationUsage( Access.class ) == null ) {
					return methodDetails;
				}
			}
		}

		final List<FieldDetails> fields = current.getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			if ( fieldDetails.hasDirectAnnotationUsage( Id.class )
					|| fieldDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				if ( fieldDetails.getDirectAnnotationUsage( Access.class ) == null ) {
					return fieldDetails;
				}
			}
		}

		return null;
	}

	private Set<ClassDetails> collectRootEntityTypes() {
		return collectRootEntityTypes( modelContext.getClassDetailsRegistry() );
	}

	private static Set<ClassDetails> collectRootEntityTypes(ClassDetailsRegistry classDetailsRegistry) {
		final Set<ClassDetails> collectedTypes = new HashSet<>();

		classDetailsRegistry.forEachClassDetails( (managedType) -> {
			if ( managedType.getDirectAnnotationUsage( Entity.class ) != null
					&& isRoot( managedType ) ) {
				collectedTypes.add( managedType );
			}
		} );

		return collectedTypes;
	}

	public static boolean isRoot(ClassDetails classInfo) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		if ( classInfo.getSuperClass() == null ) {
			return true;
		}

		ClassDetails current = classInfo.getSuperClass();
		while (  current != null ) {
			if ( current.getDirectAnnotationUsage( Entity.class ) != null && !current.isAbstract() ) {
				// a non-abstract super type has `@Entity` -> classInfo cannot be a root entity
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}


	/**
	 * Used in tests
	 */
	public static Set<EntityHierarchy> createEntityHierarchies(ModelCategorizationContext processingContext) {
		return new EntityHierarchyBuilder( processingContext ).process(
				collectRootEntityTypes( processingContext.getClassDetailsRegistry() ),
				EntityHierarchyBuilder::ignore
		);
	}

	private static void ignore(IdentifiableTypeMetadata it) {}
}
