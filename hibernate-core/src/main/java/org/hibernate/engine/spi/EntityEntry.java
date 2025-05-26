/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.util.ImmutableBitSet;
import org.hibernate.persister.entity.EntityPersister;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information about the current state of a managed entity instance with respect
 * to its persistent state.
 *
 * @implNote Hibernate instantiates very many of instances of this type,
 *           and so we need to take care of its impact on memory consumption.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
public interface EntityEntry {
	LockMode getLockMode();

	void setLockMode(LockMode lockMode);

	Status getStatus();

	void setStatus(Status status);

	Object getId();

	Object[] getLoadedState();

	Object getLoadedValue(String propertyName);

	void overwriteLoadedStateCollectionValue(String propertyName, PersistentCollection<?> collection);

	Object[] getDeletedState();

	void setDeletedState(Object[] deletedState);

	boolean isExistsInDatabase();

	Object getVersion();

	void postInsert(Object version);

	EntityPersister getPersister();

	/**
	 * Get the {@link EntityKey} for this entry.
	 *
	 * @return the {@link EntityKey}
	 * @throws IllegalStateException if {@link #getId()} is null
	 */
	EntityKey getEntityKey();

	String getEntityName();

	boolean isBeingReplicated();

	Object getRowId();

	void postLoad(Object entity);

	/**
	 * Handle updating the internal state of the entry after actually performing
	 * the database update. Specifically, we update the snapshot information and
	 * escalate the lock mode.
	 *
	 * @param entity The entity instance
	 * @param updatedState The state calculated after the update (becomes the
	 * new {@link #getLoadedState() loaded state}.
	 * @param nextVersion The new version.
	 */
	void postUpdate(Object entity, Object[] updatedState, Object nextVersion);

	/**
	 * After actually deleting a row, record the fact that the instance no longer
	 * exists in the database.
	 */
	void postDelete();

	/**
	 * After actually inserting a row, record the fact that the instance exists
	 * in the database (needed for identity column key generation).
	 */
	void postInsert(Object[] insertedState);

	boolean isNullifiable(boolean earlyInsert, SharedSessionContractImplementor session);

	/**
	 * Returns {@code true} if the entity can possibly be dirty. This can only
	 * be the case if it is in a modifiable state (not read-only nor deleted)
	 * and it either has mutable properties or field-interception is not telling
	 * us that it is dirty.
	 *
	 * @param entity The entity to test
	 *
	 * @return {@code true} indicates that the entity could possibly be dirty
	 *         and that the dirty-check should happen;
	 *         {@code false} indicates there is no way the entity can be dirty
	 */
	boolean requiresDirtyCheck(Object entity);

	/**
	 * Can the entity be modified?
	 * <p>
	 * The entity is modifiable if all the following are true:
	 * <ul>
	 * <li>the entity class is mutable,
	 * <li>the entity is not read-only, and
	 * <li>if the current status is {@link Status#DELETED},
	 *     then the entity was not read-only when it was deleted.
	 * </ul>
	 *
	 * @return {@code true}, if the entity is modifiable;
	 *         {@code false}, otherwise,
	 */
	boolean isModifiableEntity();

	void forceLocked(Object entity, Object nextVersion);

	boolean isReadOnly();

	void setReadOnly(boolean readOnly, Object entity);

	/**
	 * Has a bit set for every attribute position that is potentially lazy.
	 * When {@code null}, no knowledge is available and every attribute must be assumed potentially lazy.
	 */
	@Internal
	@Nullable ImmutableBitSet getMaybeLazySet();

	@Internal
	void setMaybeLazySet(@Nullable ImmutableBitSet maybeLazySet);

	@Override
	String toString();

	/**
	 * Custom serialization routine used during serialization of a
	 * {@code Session}/{@code PersistenceContext} for increased
	 * performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws IOException If a stream error occurs
	 */
	void serialize(ObjectOutputStream oos) throws IOException;

	//the following methods are handling extraState contracts.
	//they are not shared by a common superclass to avoid alignment padding
	//we are trading off duplication for padding efficiency
	void addExtraState(EntityEntryExtraState extraState);

	<T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType);
}
