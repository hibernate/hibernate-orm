/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * Models information used across the entire entity inheritance hierarchy.
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	// todo : integrate this with org.hibernate.persister.walking

	/**
	 * Access to the root entity for this hierarchy.
	 *
	 * @return The root entity for this hierarchy.
	 */
	EntityPersister getRootEntityPersister();

	/**
	 * The strategy used to persist inheritance for this hierarchy.
	 *
	 * @return The inheritance strategy for this hierarchy.
	 */
	InheritanceStrategy getInheritanceStrategy();

	/**
	 * The entity mode in effect for this hierarchy.
	 *
	 * @return The hierarchy's EntityMode.
	 */
	EntityMode getEntityMode();

	EntityIdentifier getEntityIdentifier();

	EntityDiscriminator getEntityDiscriminator();

	EntityVersion getEntityVersion();

	OptimisticLockStyle getOptimisticLockStyle();

	TenantDiscrimination getTenantDiscrimination();

	Caching getCaching();

	Caching getNaturalIdCaching();

	/**
	 * Are entities in this hierarchy mutable?
	 * <p/>
	 * For an entity it is only valid to define the root entity of a hierarchy as
	 * mutable/immutable, hence why we store this on the EntityHierarchy as opposed
	 * to the EntityPersister
	 *
	 * @return {@code true} if the entities in this hierarchy are mutable; {@code false}
	 * if they are immutable.
	 */
	boolean isMutable();

	/**
	 * "Implicit polymorphism" is a phrase regarding how inheritance should
	 * behave when one of the entities in the hierarchy is referenced in a
	 * query.  The default behavior of Hibernate ("implicit polymorphism") is
	 * that any reference to an entity in the hierarchy also implicitly refers
	 * to all of its subclasses.
	 *
	 * @return {@code true} if implicit polymorphism is enabled; {@code false}
	 * if disabled.
	 */
	boolean isImplicitPolymorphismEnabled();

	String getWhere();
}
