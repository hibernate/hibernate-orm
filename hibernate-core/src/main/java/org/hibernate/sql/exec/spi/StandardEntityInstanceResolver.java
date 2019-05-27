/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.spi.LoadingEntityEntry;

/**
 * @author Steve Ebersole
 */
public class StandardEntityInstanceResolver {
	private StandardEntityInstanceResolver() {
	}

	public static Object resolveEntityInstance(
			EntityKey entityKey,
			boolean eager,
			SharedSessionContractImplementor session) {
		// First, look for it in the PC as a managed entity
		final Object managedEntity = session.getPersistenceContext().getEntity( entityKey );
		if ( managedEntity != null ) {
			// todo (6.0) : check status?  aka, return deleted entities?
			return managedEntity;
		}

		// Next, check currently loading entities
		final LoadingEntityEntry loadingEntry = session.getPersistenceContext()
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );
		if ( loadingEntry != null ) {
			return loadingEntry.getEntityInstance();
		}

		// Lastly, try to load from database
		return session.internalLoad(
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				eager,
				false
		);
	}
}
