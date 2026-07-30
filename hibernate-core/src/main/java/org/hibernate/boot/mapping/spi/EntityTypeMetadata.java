/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

/// Read-only categorized description of an entity type and its entity-level
/// mapping options.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EntityTypeMetadata extends IdentifiableTypeMetadata {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.ENTITY;
	}

	/// The Hibernate entity name.
	String getEntityName();

	/// The Jakarta Persistence entity name.
	String getJpaEntityName();

	/// Whether this entity is the root entity of its hierarchy.
	default boolean isHierarchyRoot() {
		return this == getHierarchy().getRoot();
	}

	/// The nearest persistent entity supertype name, or `null` when this entity
	/// has no entity supertype.
	default String getSuperEntityName() {
		IdentifiableTypeMetadata superType = getSuperType();
		while ( superType != null ) {
			if ( superType instanceof EntityTypeMetadata superEntity ) {
				return superEntity.getEntityName();
			}
			superType = superType.getSuperType();
		}
		return null;
	}

	/// Whether instances of this entity are considered mutable.
	boolean isMutable();

	/// Whether this entity is eligible for second-level caching.
	boolean isCacheable();

	/// Additional table names whose changes invalidate queries involving this
	/// entity.
	String[] getSynchronizedTableNames();

	/// The configured batch-fetch size.
	int getBatchSize();

	/// Whether inserts use dynamic SQL generation.
	boolean isDynamicInsert();

	/// Whether updates use dynamic SQL generation.
	boolean isDynamicUpdate();
}
