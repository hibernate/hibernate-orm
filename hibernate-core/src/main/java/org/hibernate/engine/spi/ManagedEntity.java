/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

/**
 * Specialized {@link Managed} contract for entity classes.  Essentially provides access to information
 * about an instance's association to a Session/EntityManager.  Specific information includes:<ul>
 *     <li>
 *        the association's {@link EntityEntry} (by way of {@link #hibernate_getEntityEntry()} and
 *        {@link #hibernate_setEntityEntry}).  EntityEntry describes states, snapshots, etc.
 *     </li>
 *     <li>
 *         link information.  ManagedEntity instances are part of a "linked list", thus link information
 *         describes the next and previous entries/nodes in that ordering.  See
 *         {@link #hibernate_getNextManagedEntity}, {@link #hibernate_setNextManagedEntity},
 *         {@link #hibernate_getPreviousManagedEntity()}, {@link #hibernate_setPreviousManagedEntity}
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface ManagedEntity extends Managed {
	/**
	 * Obtain a reference to the entity instance.
	 *
	 * @return The entity instance.
	 */
	public Object hibernate_getEntityInstance();

	/**
	 * Provides access to the associated EntityEntry.
	 *
	 * @return The EntityEntry associated with this entity instance.
	 *
	 * @see #hibernate_setEntityEntry
	 */
	public EntityEntry hibernate_getEntityEntry();

	/**
	 * Injects the EntityEntry associated with this entity instance.  The EntityEntry represents state associated
	 * with the entity in regards to its association with a Hibernate Session.
	 *
	 * @param entityEntry The EntityEntry associated with this entity instance.
	 */
	public void hibernate_setEntityEntry(EntityEntry entityEntry);

	public ManagedEntity hibernate_getPreviousManagedEntity();

	public void hibernate_setPreviousManagedEntity(ManagedEntity previous);

	public ManagedEntity hibernate_getNextManagedEntity();

	public void hibernate_setNextManagedEntity(ManagedEntity next);
}
