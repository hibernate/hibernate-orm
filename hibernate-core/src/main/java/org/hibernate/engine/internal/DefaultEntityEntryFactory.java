/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Factory for the safe approach implementation of {@link org.hibernate.engine.spi.EntityEntry}.
 * <p>
 * Smarter implementations could store less state.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DefaultEntityEntryFactory implements EntityEntryFactory {
	@Override
	public EntityEntry createEntityEntry(Status status, Object[] loadedState, Object rowId, Serializable id, Object version, LockMode lockMode, boolean existsInDatabase, EntityPersister persister, boolean disableVersionIncrement, boolean lazyPropertiesAreUnfetched, PersistenceContext persistenceContext) {
		return new DefaultEntityEntry(
				status,
				loadedState,
				rowId,
				id,
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement,
				lazyPropertiesAreUnfetched,
				persistenceContext
		);
	}
}
