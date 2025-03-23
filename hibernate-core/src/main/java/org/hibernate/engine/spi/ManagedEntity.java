/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public interface ManagedEntity extends Managed, InstanceIdentity {
	/**
	 * Obtain a reference to the entity instance.
	 *
	 * @return The entity instance.
	 */
	Object $$_hibernate_getEntityInstance();

	/**
	 * Provides access to the associated EntityEntry.
	 *
	 * @return The EntityEntry associated with this entity instance.
	 *
	 * @see #$$_hibernate_setEntityEntry
	 */
	EntityEntry $$_hibernate_getEntityEntry();

	/**
	 * Injects the EntityEntry associated with this entity instance.  The EntityEntry represents state associated
	 * with the entity in regards to its association with a Hibernate Session.
	 *
	 * @param entityEntry The EntityEntry associated with this entity instance.
	 */
	void $$_hibernate_setEntityEntry(EntityEntry entityEntry);

	/**
	 * Part of entry linking; obtain reference to the previous entry.  Can be {@code null}, which should indicate
	 * this is the head node.
	 *
	 * @return The previous entry
	 */
	ManagedEntity $$_hibernate_getPreviousManagedEntity();

	/**
	 * Part of entry linking; sets the previous entry.  Again, can be {@code null}, which should indicate
	 * this is (now) the head node.
	 *
	 * @param previous The previous entry
	 */
	void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous);

	/**
	 * Part of entry linking; obtain reference to the next entry.  Can be {@code null}, which should indicate
	 * this is the tail node.
	 *
	 * @return The next entry
	 */
	ManagedEntity $$_hibernate_getNextManagedEntity();

	/**
	 * Part of entry linking; sets the next entry.  Again, can be {@code null}, which should indicate
	 * this is (now) the tail node.
	 *
	 * @param next The next entry
	 */
	void $$_hibernate_setNextManagedEntity(ManagedEntity next);

	/**
	 * Used to understand if the tracker can be used to detect dirty properties.
	 *
	 * <pre>
	 * &#64;Entity
	 * class MyEntity{
	 * 	&#64;Id Integer id
	 * 	String name
	 * }
	 *
	 * inSession (
	 * 	session -> {
	 * 		MyEntity entity = new MyEntity(1, "Poul");
	 * 		session.persist(entity);
	 * });
	 *
	 *
	 * inSession (
	 * 	session -> {
	 * 		MyEntity entity = new MyEntity(1, null);
	 * 		session.merge(entity);
	 * });
	 * </pre>
	 * Because the attribute `name` has been set to null the SelfDirtyTracker
	 * does not detect any change and so doesn't mark the attribute as dirty
	 * so the merge does not perform any update.
	 *
	 *
	 * @param useTracker true if the tracker can be used to detect dirty properties, false otherwise
	 *
	 */
	void $$_hibernate_setUseTracker(boolean useTracker);

	/**
	 * Can the tracker be used to detect dirty attributes
	 *
	 * @return true if the tracker can be used to detect dirty properties, false otherwise
	 */
	boolean $$_hibernate_useTracker();

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default ManagedEntity asManagedEntity() {
		return this;
	}

	/**
	 * Utility method that allows injecting all persistence-related information on the managed entity at once.
	 *
	 * @param entityEntry the {@link EntityEntry} associated with this entity instance
	 * @param previous the previous entry
	 * @param next the next entry
	 * @param instanceId unique identifier for this instance
	 * @return the previous {@link EntityEntry} contained in this managed entity, or {@code null}
	 * @see #$$_hibernate_setEntityEntry(EntityEntry)
	 * @see #$$_hibernate_setPreviousManagedEntity(ManagedEntity)
	 * @see #$$_hibernate_setNextManagedEntity(ManagedEntity)
	 * @see #$$_hibernate_setInstanceId(int)
	 * @since 7.0
	 */
	default EntityEntry $$_hibernate_setPersistenceInfo(EntityEntry entityEntry, ManagedEntity previous, ManagedEntity next, int instanceId) {
		final EntityEntry oldEntry = $$_hibernate_getEntityEntry();
		$$_hibernate_setEntityEntry( entityEntry );
		$$_hibernate_setPreviousManagedEntity( previous );
		$$_hibernate_setNextManagedEntity( next );
		$$_hibernate_setInstanceId( instanceId );
		return oldEntry;
	}
}
