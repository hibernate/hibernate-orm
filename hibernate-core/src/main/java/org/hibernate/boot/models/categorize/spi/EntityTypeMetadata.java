/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Map;

import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.naming.EntityNaming;

/**
 * Metadata about an {@linkplain jakarta.persistence.metamodel.EntityType entity type}
 *
 * @author Steve Ebersole
 */
public interface EntityTypeMetadata extends IdentifiableTypeMetadata, EntityNaming {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.ENTITY;
	}

	/**
	 * The Hibernate notion of entity-name, used for dynamic models
	 */
	String getEntityName();

	/**
	 * The JPA notion of entity-name, used for HQL references (import)
	 */
	String getJpaEntityName();

	/**
	 * Whether the state of the entity is written to the database (mutable) or not (immutable)
	 */
	boolean isMutable();

	/**
	 * Whether this entity is cacheable.
	 *
	 * @see jakarta.persistence.Cacheable
	 * @see org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor#getSharedCacheMode()
	 */
	boolean isCacheable();

	/**
	 * Any tables to which this entity maps that Hibernate does not know about.
	 *
	 * @see org.hibernate.annotations.View
	 * @see org.hibernate.annotations.Subselect
	 */
	String[] getSynchronizedTableNames();

	/**
	 * A size to use for the entity with batch loading
	 */
	int getBatchSize();

	/**
	 * Whether to perform a select prior to performing a {@linkplain org.hibernate.Session#update}
	 *
	 * @deprecated Because {@linkplain org.hibernate.Session#update} itself is deprecated
	 */
	@Deprecated
	boolean isSelectBeforeUpdate();

	/**
	 * Whether to perform dynamic inserts.
	 *
	 * @see org.hibernate.annotations.DynamicInsert
	 */
	boolean isDynamicInsert();

	/**
	 * Whether to perform dynamic updates.
	 *
	 * @see org.hibernate.annotations.DynamicUpdate
	 */
	boolean isDynamicUpdate();

	/**
	 * Custom SQL to perform an INSERT of this entity
	 */
	Map<String, CustomSql> getCustomInserts();

	/**
	 * Custom SQL to perform an UPDATE of this entity
	 */
	Map<String, CustomSql> getCustomUpdates();

	/**
	 * Custom SQL to perform an DELETE of this entity
	 */
	Map<String, CustomSql> getCustomDeletes();
}
