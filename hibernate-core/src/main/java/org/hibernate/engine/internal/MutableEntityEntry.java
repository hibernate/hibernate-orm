/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An EntityEntry implementation for mutable entities.
 *
 * @author Gavin King
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 * @author Gunnar Morling
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero </a>
 */
public final class MutableEntityEntry extends AbstractEntityEntry {
	/**
	 * @deprecated the tenantId and entityMode parameters where removed: this constructor accepts but ignores them.
	 * Use the other constructor!
	 */
	@Deprecated
	public MutableEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final EntityMode entityMode,
			final String tenantId,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext) {
		this( status, loadedState, rowId, id, version, lockMode, existsInDatabase,
				persister,disableVersionIncrement, persistenceContext
		);
	}

	public MutableEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
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
	@SuppressWarnings( {"JavaDoc"})
	private MutableEntityEntry(
			final SessionFactoryImplementor factory,
			final String entityName,
			final Serializable id,
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
	 * @throws java.io.IOException If a stream error occurs
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
				(Serializable) ois.readObject(),
				Status.valueOf( (String) ois.readObject() ),
				( previousStatusString = (String) ois.readObject() ).length() == 0
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
