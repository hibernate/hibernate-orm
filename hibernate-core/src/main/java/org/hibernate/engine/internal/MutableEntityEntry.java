/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An {@link EntityEntry} implementation for mutable entities.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
public final class MutableEntityEntry extends AbstractEntityEntry {

	public MutableEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Object id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext) {
		super( status, loadedState, rowId, id, version, lockMode, existsInDatabase, persister,
				disableVersionIncrement, persistenceContext
		);
	}

	/**
	 * This for is used during custom deserialization handling
	 */
	private MutableEntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Object id,
			final Status status,
			final Status previousStatus,
			final Object[] loadedState,
			final Object[] deletedState,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final boolean isBeingReplicated,
			final PersistenceContext persistenceContext) {
		super( factory, entityName, id, status, previousStatus, loadedState, deletedState,
				version, lockMode, existsInDatabase, isBeingReplicated, persistenceContext
		);
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param persistenceContext The context being deserialized.
	 *
	 * @return The deserialized EntityEntry
	 *
	 * @throws IOException If a stream error occurs
	 * @throws ClassNotFoundException If any of the classes declared in the stream
	 * cannot be found
	 */
	public static EntityEntry deserialize(
			ObjectInputStream ois,
			PersistenceContext persistenceContext) throws IOException, ClassNotFoundException {
		String previousStatusString;
		return new MutableEntityEntry(
				persistenceContext.getSession().getFactory(),
				(String) ois.readObject(),
				ois.readObject(),
				Status.valueOf( (String) ois.readObject() ),
				( previousStatusString = (String) ois.readObject() )
						.isEmpty()
							? null
							: Status.valueOf( previousStatusString ),
				(Object[]) ois.readObject(),
				(Object[]) ois.readObject(),
				ois.readObject(),
				LockMode.valueOf( (String) ois.readObject() ),
				ois.readBoolean(),
				ois.readBoolean(),
				persistenceContext
		);
	}
}
