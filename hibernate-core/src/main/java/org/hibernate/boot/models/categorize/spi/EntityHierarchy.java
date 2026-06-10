/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;
import org.hibernate.engine.OptimisticLockStyle;

import jakarta.persistence.InheritanceType;

/// Models an entity hierarchy comprised of {@linkplain EntityTypeMetadata entity}
/// and visible {@linkplain MappedSuperclassTypeMetadata mapped-superclass} types.
///
/// The hierarchy root is always the root entity.  The absolute root is the highest
/// visible identifiable type in the same inheritance chain, which may be a
/// mapped-superclass above the root entity.  Mapped-superclasses that are present in
/// the persistence unit but are not visible from an entity hierarchy are not part of
/// an {@code EntityHierarchy}.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EntityHierarchy {
	/// The hierarchy's root entity type.
	@Nonnull
	EntityTypeMetadata getRoot();

	/// The highest visible identifiable type in this hierarchy, which might be a
	/// mapped-superclass above the {@linkplain #getRoot() root entity}.
	@Nonnull
	IdentifiableTypeMetadata getAbsoluteRoot();

	/// Visit each type in the hierarchy, top down starting from {@linkplain #getAbsoluteRoot()}
	void forEachType(HierarchyTypeVisitor typeVisitor);

	/// The inheritance strategy for the hierarchy.
	@Nonnull
	InheritanceType getInheritanceType();

	/// The "default access-type" effective for the hierarchy.
	/// See Jakarta Persistence section _2.3.1. Default Access Type_.
	@Nonnull
	AccessType getDefaultAccessType();

	@Nonnull
	KeyMapping getIdMapping();

	@Nullable
	KeyMapping getNaturalIdMapping();

	@Nullable
	AttributeMetadata getVersionAttribute();

	@Nullable
	AttributeMetadata getTenantIdAttribute();

	/// Style of optimistic locking for the hierarchy.
	@Nonnull
	OptimisticLockStyle getOptimisticLockStyle();

	/// The caching configuration for entities in this hierarchy.
	@Nonnull
	CacheRegion getCacheRegion();

	/// The caching configuration for this hierarchy's {@linkplain org.hibernate.annotations.NaturalId natural-id}
	@Nonnull
	NaturalIdCacheRegion getNaturalIdCacheRegion();

	/// Describes a type's place in the hierarchy relative to the {@linkplain #getRoot() root entity}
	enum HierarchyRelation { SUPER, ROOT, SUB }

	@FunctionalInterface
	interface HierarchyTypeVisitor {
		void visitType(
				IdentifiableTypeMetadata type,
				IdentifiableTypeMetadata superType,
				EntityHierarchy hierarchy,
				HierarchyRelation relation);
	}
}
