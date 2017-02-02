/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * Specialized {@link Managed} contract for entity classes.  Essentially provides access to information
 * about an instance's association to a Session/EntityManager.  Specific information includes:<ul>
 *     <li>
 *        the association's {@link EntityEntry} (by way of {@link #$$_hibernate_getEntityEntry} and
 *        {@link #$$_hibernate_setEntityEntry}).  EntityEntry describes states, snapshots, etc.
 *     </li>
 *     <li>
 *         link information.  ManagedEntity instances are part of a "linked list", thus link information
 *         describes the next and previous entries/nodes in that ordering.  See
 *         {@link #$$_hibernate_getNextManagedEntity}, {@link #$$_hibernate_setNextManagedEntity},
 *         {@link #$$_hibernate_getPreviousManagedEntity}, {@link #$$_hibernate_setPreviousManagedEntity}
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
	public Object $$_hibernate_getEntityInstance();

	/**
	 * Provides access to the associated EntityEntry.
	 *
	 * @return The EntityEntry associated with this entity instance.
	 *
	 * @see #$$_hibernate_setEntityEntry
	 */
	public EntityEntry $$_hibernate_getEntityEntry();

	/**
	 * Injects the EntityEntry associated with this entity instance.  The EntityEntry represents state associated
	 * with the entity in regards to its association with a Hibernate Session.
	 *
	 * @param entityEntry The EntityEntry associated with this entity instance.
	 */
	public void $$_hibernate_setEntityEntry(EntityEntry entityEntry);

	/**
	 * Part of entry linking; obtain reference to the previous entry.  Can be {@code null}, which should indicate
	 * this is the head node.
	 *
	 * @return The previous entry
	 */
	public ManagedEntity $$_hibernate_getPreviousManagedEntity();

	/**
	 * Part of entry linking; sets the previous entry.  Again, can be {@code null}, which should indicate
	 * this is (now) the head node.
	 *
	 * @param previous The previous entry
	 */
	public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous);

	/**
	 * Part of entry linking; obtain reference to the next entry.  Can be {@code null}, which should indicate
	 * this is the tail node.
	 *
	 * @return The next entry
	 */
	public ManagedEntity $$_hibernate_getNextManagedEntity();

	/**
	 * Part of entry linking; sets the next entry.  Again, can be {@code null}, which should indicate
	 * this is (now) the tail node.
	 *
	 * @param next The next entry
	 */
	public void $$_hibernate_setNextManagedEntity(ManagedEntity next);
}
