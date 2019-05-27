/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;

/**
 * @author Steve Ebersole
 */
public interface ExecutionContext {
	default Object resolveEntityInstance(EntityKey entityKey, boolean eager) {
		return StandardEntityInstanceResolver.resolveEntityInstance(
				entityKey,
				eager,
				getSession()
		);
	}

	SharedSessionContractImplementor getSession();

	QueryOptions getQueryOptions();

	default LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}

	DomainParameterBindingContext getDomainParameterBindingContext();

	Callback getCallback();

	/**
	 * Get the collection key for the collection which is to be loaded immediately.
	 */
	default CollectionKey getCollectionKey() {
		return null;
	}
}
