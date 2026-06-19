/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.annotation.Nonnull;
import org.hibernate.boot.models.AccessTypeDeterminationException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;

/// Builds [EntityHierarchy] references from
/// {@linkplain ClassDetailsRegistry#forEachClassDetails managed classes}.
///
/// @since 9.0
/// @author Steve Ebersole
public class EntityHierarchyBuilder {

	public static Set<EntityHierarchy> createEntityHierarchies(
			ManagedTypeInheritanceState inheritanceState,
			CategorizationContext buildingContext) {
		return new EntityHierarchyBuilder( buildingContext ).process( inheritanceState );
	}

	private final CategorizationContext modelContext;

	public EntityHierarchyBuilder(CategorizationContext modelContext) {
		this.modelContext = modelContext;
	}

	private Set<EntityHierarchy> process(
			ManagedTypeInheritanceState inheritanceState,
			MappedSuperclassTracker mappedSuperclassTracker) {
		final Set<ClassDetails> rootEntities = inheritanceState.getRootEntities();
		final Set<EntityHierarchy> hierarchies = CollectionHelper.setOfSize( rootEntities.size() );

		rootEntities.forEach( (rootEntity) -> {
			if ( hasEntitySuperType( rootEntity, inheritanceState ) ) {
				return;
			}
			final AccessType defaultAccessType = determineDefaultAccessTypeForHierarchy( rootEntity );
			hierarchies.add( new EntityHierarchyImpl(
					rootEntity,
					defaultAccessType,
					org.hibernate.cache.spi.access.AccessType.TRANSACTIONAL,
					inheritanceState,
					mappedSuperclassTracker,
					modelContext
			) );
		} );

		return hierarchies;
	}

	private boolean hasEntitySuperType(
			ClassDetails classDetails,
			ManagedTypeInheritanceState inheritanceState) {
		ClassDetails superType = inheritanceState.getSuperType( classDetails );
		while ( superType != null ) {
			if ( CategorizationHelper.isEntity( superType ) ) {
				return true;
			}
			superType = inheritanceState.getSuperType( superType );
		}
		return false;
	}

	private Set<EntityHierarchy> process(ManagedTypeInheritanceState inheritanceState) {
		final MappedSuperclassTracker mappedSuperclassTracker = new MappedSuperclassTracker( inheritanceState );
		final Set<EntityHierarchy> entityHierarchies = process(
				inheritanceState,
				mappedSuperclassTracker
		);
		mappedSuperclassTracker.warnAboutUnusedMappedSuperclasses();
		return entityHierarchies;
	}

	@Nonnull
	private AccessType determineDefaultAccessTypeForHierarchy(ClassDetails rootEntityType) {
		assert rootEntityType != null;

		ClassDetails current = rootEntityType;
		while ( current != null ) {
			var inclusiveMember = findDefaultedMember( current );
			if ( inclusiveMember == null ) {
				current = current.getSuperClass();
				continue;
			}

			if ( inclusiveMember.getKind() == AnnotationTarget.Kind.FIELD ) {
				return AccessType.FIELD;
			}
			else if ( inclusiveMember.getKind() == AnnotationTarget.Kind.METHOD
					&& inclusiveMember.asMethodDetails().getMethodKind() == MethodDetails.MethodKind.GETTER ) {
				return AccessType.PROPERTY;
			}
			else {
				// this should never happen because of the nature of the checks in findDefaultedMember()...
				throw new AccessTypeDeterminationException( rootEntityType );
			}

		}

		return modelContext.getEffectiveMappingDefaults().getDefaultPropertyAccessType();
	}

	protected MemberDetails findDefaultedMember(ClassDetails current) {
		// For now, keep using the old approach of looking for id.
		// But ultimately we may want to pivot away to a more JPA way
		// looking for any attribute without `@Access` (identifiers could
		// have `@Access` which should in theory exclude them from consideration).
		return determineIdMember( current );
	}

	private MemberDetails determineIdMember(ClassDetails current) {
		final List<MethodDetails> methods = current.getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			if ( methodDetails.hasDirectAnnotationUsage( Id.class )
					|| methodDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				return methodDetails;
			}
		}

		final List<FieldDetails> fields = current.getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			if ( fieldDetails.hasDirectAnnotationUsage( Id.class )
					|| fieldDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				return fieldDetails;
			}
		}

		return null;
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
		while ( current != null ) {
			if ( current.hasDirectAnnotationUsage( Entity.class ) ) {
				// a super type has `@Entity`, cannot be root
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}
}
