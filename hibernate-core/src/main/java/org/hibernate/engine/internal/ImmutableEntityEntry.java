/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.UnsupportedLockAttemptException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.internal.AbstractEntityEntry.EnumState.LOCK_MODE;

/**
 * An {@link EntityEntry} implementation for immutable entities.
 *
 * @implNote Note that this implementation is not completely immutable in terms of its internal state;
 *           the term immutable here refers to the entity it describes.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Sanne Grinovero
 *
 * @see org.hibernate.annotations.Immutable
 */
public final class ImmutableEntityEntry extends AbstractEntityEntry {
	public ImmutableEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Object id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {

		super(
				status,
				loadedState,
				rowId,
				id,
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement,
				// purposefully do not pass along the session/persistence-context : HHH-10251
				null
		);
	}

	/**
	 * This for is used during custom deserialization handling
	 */
	private ImmutableEntityEntry(
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

	@Override
	public void setLockMode(LockMode lockMode) {
		if ( lockMode.greaterThan(LockMode.READ) ) {
			throw new UnsupportedLockAttemptException( "Lock mode "
					+ lockMode + " not supported for read-only entity" );
		}
		else {
			setCompressedValue( LOCK_MODE, lockMode );
		}
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
		return new ImmutableEntityEntry(
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
				null
		);
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		throw new AssertionFailure( "Session/PersistenceContext is not available from an ImmutableEntityEntry" );
	}

}
