/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

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
		final EntityHolder holder = session.getPersistenceContext().getEntityHolder( entityKey );
		if ( holder != null && holder.isEventuallyInitialized() ) {
			return holder.getEntity();
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
