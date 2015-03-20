/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat Inc. or third-party contributors as
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;

/**
 * We need an entry to tell us all about the current state of an object with respect to its persistent state
 *
 * Implementation Warning: Hibernate needs to instantiate a high amount of instances of this class,
 * therefore we need to take care of its impact on memory consumption.
 *
 * @author Gavin King
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public interface EntityEntry {
	LockMode getLockMode();

	void setLockMode(LockMode lockMode);

	Status getStatus();

	void setStatus(Status status);

	Serializable getId();

	Object[] getLoadedState();

	Object[] getDeletedState();

	void setDeletedState(Object[] deletedState);

	boolean isExistsInDatabase();

	Object getVersion();

	EntityPersister getPersister();

	/**
	 * Get the EntityKey based on this EntityEntry.
	 * @return the EntityKey
	 * @throws  IllegalStateException if getId() is null
	 */
	EntityKey getEntityKey();

	String getEntityName();

	boolean isBeingReplicated();

	Object getRowId();

	/**
	 * Handle updating the internal state of the entry after actually performing
	 * the database update.  Specifically we update the snapshot information and
	 * escalate the lock mode
	 *
	 * @param entity The entity instance
	 * @param updatedState The state calculated after the update (becomes the
	 * new {@link #getLoadedState() loaded state}.
	 * @param nextVersion The new version.
	 */
	void postUpdate(Object entity, Object[] updatedState, Object nextVersion);

	/**
	 * After actually deleting a row, record the fact that the instance no longer
	 * exists in the database
	 */
	void postDelete();

	/**
	 * After actually inserting a row, record the fact that the instance exists on the
	 * database (needed for identity-column key generation)
	 */
	void postInsert(Object[] insertedState);

	boolean isNullifiable(boolean earlyInsert, SessionImplementor session);

	Object getLoadedValue(String propertyName);

	/**
	 * Not sure this is the best method name, but the general idea here is to return {@code true} if the entity can
	 * possibly be dirty.  This can only be the case if it is in a modifiable state (not read-only/deleted) and it
	 * either has mutable properties or field-interception is not telling us it is dirty.  Clear as mud? :/
	 *
	 * A name like canPossiblyBeDirty might be better
	 *
	 * @param entity The entity to test
	 *
	 * @return {@code true} indicates that the entity could possibly be dirty and that dirty check
	 * should happen; {@code false} indicates there is no way the entity can be dirty
	 */
	boolean requiresDirtyCheck(Object entity);

	/**
	 * Can the entity be modified?
	 *
	 * The entity is modifiable if all of the following are true:
	 * <ul>
	 * <li>the entity class is mutable</li>
	 * <li>the entity is not read-only</li>
	 * <li>if the current status is Status.DELETED, then the entity was not read-only when it was deleted</li>
	 * </ul>
	 * @return true, if the entity is modifiable; false, otherwise,
	 */
	boolean isModifiableEntity();

	void forceLocked(Object entity, Object nextVersion);

	boolean isReadOnly();

	void setReadOnly(boolean readOnly, Object entity);

	@Override
	String toString();

	boolean isLoadedWithLazyPropertiesUnfetched();

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 *
	 * @throws java.io.IOException If a stream error occurs
	 */
	void serialize(ObjectOutputStream oos) throws IOException;

	//the following methods are handling extraState contracts.
	//they are not shared by a common superclass to avoid alignment padding
	//we are trading off duplication for padding efficiency
	void addExtraState(EntityEntryExtraState extraState);

	<T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType);
}
