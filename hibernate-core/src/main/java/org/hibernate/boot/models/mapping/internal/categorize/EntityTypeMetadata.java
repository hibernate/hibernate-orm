/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.mapping.CustomSqlMapping;

/// Categorized metadata about an {@linkplain jakarta.persistence.metamodel.EntityType entity type}.
///
/// Entity metadata extends identifiable type metadata with entity naming, mutability,
/// caching, synchronization, batching, and custom SQL options needed by binding.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EntityTypeMetadata extends IdentifiableTypeMetadata, EntityNaming {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.ENTITY;
	}

	/// The Hibernate notion of entity-name, used for dynamic models
	String getEntityName();

	/// The JPA notion of entity-name, used for HQL references (import)
	String getJpaEntityName();

	/// Whether the state of the entity is written to the database (mutable) or not (immutable)
	boolean isMutable();

	/// Whether this entity is cacheable.
	///
	/// @see jakarta.persistence.Cacheable
	/// @see org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor#getSharedCacheMode()
	boolean isCacheable();

	/// Any tables to which this entity maps that Hibernate does not know about.
	///
	/// @see org.hibernate.annotations.View
	/// @see org.hibernate.annotations.Subselect
	String[] getSynchronizedTableNames();

	/// A size to use for the entity with batch loading
	int getBatchSize();

	/// Whether to perform dynamic inserts.
	///
	/// @see org.hibernate.annotations.DynamicInsert
	boolean isDynamicInsert();

	/// Whether to perform dynamic updates.
	///
	/// @see org.hibernate.annotations.DynamicUpdate
	boolean isDynamicUpdate();

	/// Custom SQL to perform an INSERT of this entity
	CustomSqlMapping getCustomInsert();

	/// Custom SQL to perform an UPDATE of this entity
	CustomSqlMapping getCustomUpdate();

	/// Custom SQL to perform a DELETE of this entity
	CustomSqlMapping getCustomDelete();
}
