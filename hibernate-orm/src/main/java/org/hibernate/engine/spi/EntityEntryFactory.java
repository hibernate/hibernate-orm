/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Contract to build {@link org.hibernate.engine.spi.EntityEntry}
 *
 * @author Emmanuel Bernard
 */
public interface EntityEntryFactory extends Serializable {

	/**
	 * Creates {@link org.hibernate.engine.spi.EntityEntry}.
	 */
	EntityEntry createEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext);
}
