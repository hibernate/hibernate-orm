/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.exec.results.process.spi.InitializerSource;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityReference extends FetchParent, InitializerSource {
	/**
	 * Retrieves the entity persister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	EntityPersister getEntityPersister();

	/**
	 * Get the description of the entity's identifier, specific to this query
	 *
	 * @todo DO we need this?
	 *
	 * @return The identifier description.
	 */
	EntityIdentifierReference getIdentifierReference();


//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// org.hibernate.loader.plan.spi.EntityReference impls
//
//	@Override
//	default String getQuerySpaceUid() {
//		return null;
//	}
//
//	@Override
//	default String getTableGroupUniqueIdentifier() {
//		return null;
//	}
//
//	@Override
//	default InitializerParent getInitializerParentForFetchInitializers() {
//		return null;
//	}
//
//	@Override
//	default void addFetch(Fetch fetch) {
//
//	}
//
//	@Override
//	default EntityIdentifierDescription getIdentifierDescription() {
//		return getIdentifierReference();
//	}
//
//	@Override
//	default BidirectionalEntityReference[] getBidirectionalEntityReferences() {
//		return new BidirectionalEntityReference[0];
//	}
//
//	@Override
//	default org.hibernate.loader.plan.spi.EntityReference resolveEntityReference() {
//		return null;
//	}
}
