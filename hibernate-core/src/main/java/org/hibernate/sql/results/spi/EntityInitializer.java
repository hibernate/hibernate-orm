/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Initializer implementation for initializing entity references.
 *
 * @author Steve Ebersole
 */
public interface EntityInitializer extends Initializer, FetchParentAccess {
	EntityDescriptor getEntityInitialized();

	Object getEntityInstance();

	@Override
	default Object getFetchParentInstance() {
		final Object entityInstance = getEntityInstance();
		if ( entityInstance == null ) {
			throw new IllegalStateException( "Unexpected state condition - entity instance not yet resolved" );
		}
		return entityInstance;
	}

	/**
	 * @todo (6.0) : should this hydrate all "hierarchy state"?
	 * 		- identifier
	 * 		- rowId
	 * 		- discriminator
	 * 		- etc
	 */
	void hydrateIdentifier(RowProcessingState rowProcessingState);

	void resolveEntityKey(RowProcessingState rowProcessingState);

	void hydrateEntityState(RowProcessingState rowProcessingState);

	default void resolveEntityState(RowProcessingState rowProcessingState) {
		throw new NotYetImplementedFor6Exception(
				"is this needed?  whats the diff with #hydrateEntityState and/or Initializer#finishUp?"
		);
	}
}
