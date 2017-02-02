/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Factory for the safe approach implementation of {@link org.hibernate.engine.spi.EntityEntry}.
 * <p/>
 * Smarter implementations could store less state.
 *
 * @author Emmanuel Bernard
 */
public class ImmutableEntityEntryFactory implements EntityEntryFactory {
	/**
	 * Singleton access
	 */
	public static final ImmutableEntityEntryFactory INSTANCE = new ImmutableEntityEntryFactory();

	private ImmutableEntityEntryFactory() {
	}

	@Override
	public EntityEntry createEntityEntry(
			Status status,
			Object[] loadedState,
			Object rowId,
			Serializable id,
			Object version,
			LockMode lockMode,
			boolean existsInDatabase,
			EntityPersister persister,
			boolean disableVersionIncrement,
			PersistenceContext persistenceContext) {
		return new ImmutableEntityEntry(
				status,
				loadedState,
				rowId,
				id,
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement,
				persistenceContext
		);
	}
}
