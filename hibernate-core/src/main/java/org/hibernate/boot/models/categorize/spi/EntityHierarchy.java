/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.engine.OptimisticLockStyle;

import jakarta.persistence.AccessType;
import jakarta.persistence.InheritanceType;

/**
 * Models an entity hierarchy comprised of {@linkplain EntityTypeMetadata entity}
 * and {@linkplain MappedSuperclassTypeMetadata mapped-superclass} types.
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	/**
	 * The hierarchy's root type.
	 */
	EntityTypeMetadata getRoot();

	/**
	 * The absolute root of the hierarchy, which might be a mapped-superclass
	 * above the {@linkplain #getRoot() root entity}
	 */
	IdentifiableTypeMetadata getAbsoluteRoot();

	/**
	 * Visit each type in the hierarchy, top down starting from {@linkplain #getAbsoluteRoot()}
	 */
	void forEachType(HierarchyTypeVisitor typeVisitor);

	/**
	 * The default access-type for the hierarchy.  See section <i>2.3.1 Default Access Type</i>
	 * of the Jakarta Persistence specification.
	 */
	AccessType getDefaultAccessType();

	/**
	 * The inheritance strategy for the hierarchy.
	 */
	InheritanceType getInheritanceType();

	KeyMapping getIdMapping();

	KeyMapping getNaturalIdMapping();

	AttributeMetadata getVersionAttribute();

	AttributeMetadata getTenantIdAttribute();

	/**
	 * Style of optimistic locking for the hierarchy.
	 */
	OptimisticLockStyle getOptimisticLockStyle();

	/**
	 * The caching configuration for entities in this hierarchy.
	 */
	CacheRegion getCacheRegion();

	/**
	 * The caching configuration for this hierarchy's {@linkplain org.hibernate.annotations.NaturalId natural-id}
	 */
	NaturalIdCacheRegion getNaturalIdCacheRegion();

	/**
	 * Describes a type's place in the hierarchy relative to the {@linkplain #getRoot() root entity}
	 */
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
