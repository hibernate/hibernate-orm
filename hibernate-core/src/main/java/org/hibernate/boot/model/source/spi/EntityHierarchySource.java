/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.Caching;
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
	public EntitySource getRoot();

	/**
	 * The inheritance type/strategy for the hierarchy.
	 * <p/>
	 * NOTE : The entire hierarchy must comply with the same inheritance strategy.
	 *
	 * @return The inheritance type.
	 */
	public InheritanceType getHierarchyInheritanceType();

	/**
	 * Obtain source information about this entity's identifier.
	 *
	 * @return Identifier source information.
	 */
	public IdentifierSource getIdentifierSource();

	/**
	 * Obtain the source information about the attribute used for optimistic locking.
	 *
	 * @return the source information about the attribute used for optimistic locking
	 */
	public VersionAttributeSource getVersionAttributeSource();

	/**
	 * Obtain the source information about the discriminator attribute for single table inheritance
	 *
	 * @return the source information about the discriminator attribute for single table inheritance
	 */
	public DiscriminatorSource getDiscriminatorSource();

	/**
	 * Obtain the source information about the multi-tenancy discriminator for this entity
	 *
	 * @return the source information about the multi-tenancy discriminator for this entity
	 */
	public MultiTenancySource getMultiTenancySource();

	/**
	 * Obtain the entity mode for this entity.
	 *
	 * @return The entity mode.
	 */
	public EntityMode getEntityMode();

	/**
	 * Is this root entity mutable?
	 *
	 * @return {@code true} indicates mutable; {@code false} non-mutable.
	 */
	public boolean isMutable();

	/**
	 * Should explicit polymorphism (querying) be applied to this entity?
	 *
	 * @return {@code true} indicates explicit polymorphism; {@code false} implicit.
	 */
	public boolean isExplicitPolymorphism();

	/**
	 * Obtain the specified extra where condition to be applied to this entity.
	 *
	 * @return The extra where condition
	 */
	public String getWhere();

	/**
	 * Obtain the row-id name for this entity
	 *
	 * @return The row-id name
	 */
	public String getRowId();

	/**
	 * Obtain the optimistic locking style for this entity.
	 *
	 * @return The optimistic locking style.
	 */
	public OptimisticLockStyle getOptimisticLockStyle();

	/**
	 * Obtain the caching configuration for this entity.
	 *
	 * @return The caching configuration.
	 */
	public Caching getCaching();

	/**
	 * Obtain the natural id caching configuration for this entity.
	 *
	 * @return The natural id caching configuration.
	 */
	public Caching getNaturalIdCaching();
}
