/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.engine.OptimisticLockStyle;

/**
 * Models the source-agnostic view of an entity hierarchy.
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchySource {
	/**
	 * Obtain the hierarchy's root type source.
	 *
	 * @return The root type source.
	 */
	EntitySource getRoot();

	/**
	 * The inheritance type/strategy for the hierarchy.
	 * <p>
	 * The entire hierarchy must have with the same inheritance strategy.
	 *
	 * @return The inheritance type.
	 */
	InheritanceType getHierarchyInheritanceType();

	/**
	 * Obtain source information about this entity's identifier.
	 *
	 * @return Identifier source information.
	 */
	IdentifierSource getIdentifierSource();

	/**
	 * Obtain the source information about the attribute used for optimistic locking.
	 *
	 * @return the source information about the attribute used for optimistic locking
	 */
	VersionAttributeSource getVersionAttributeSource();

	/**
	 * Obtain the source information about the discriminator attribute for single table inheritance
	 *
	 * @return the source information about the discriminator attribute for single table inheritance
	 */
	DiscriminatorSource getDiscriminatorSource();

	/**
	 * Obtain the source information about the multi-tenancy discriminator for this entity
	 *
	 * @return the source information about the multi-tenancy discriminator for this entity
	 */
	MultiTenancySource getMultiTenancySource();

	/**
	 * Is this root entity mutable?
	 *
	 * @return {@code true} indicates mutable; {@code false} non-mutable.
	 */
	boolean isMutable();

	/**
	 * Should explicit polymorphism (querying) be applied to this entity?
	 *
	 * @return {@code true} indicates explicit polymorphism; {@code false} implicit.
	 */
	boolean isExplicitPolymorphism();

	/**
	 * Obtain the specified extra where condition to be applied to this entity.
	 *
	 * @return The extra where condition
	 */
	String getWhere();

	/**
	 * Obtain the row-id name for this entity
	 *
	 * @return The row-id name
	 */
	String getRowId();

	/**
	 * Obtain the optimistic locking style for this entity.
	 *
	 * @return The optimistic locking style.
	 */
	OptimisticLockStyle getOptimisticLockStyle();

	/**
	 * Obtain the caching configuration for this entity.
	 *
	 * @return The caching configuration.
	 */
	Caching getCaching();

	/**
	 * Obtain the natural id caching configuration for this entity.
	 *
	 * @return The natural id caching configuration.
	 */
	Caching getNaturalIdCaching();
}
