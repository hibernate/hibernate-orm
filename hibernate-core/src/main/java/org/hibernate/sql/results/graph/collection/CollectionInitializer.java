/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Initializer implementation for initializing collections (plural attributes)
 *
 * @author Steve Ebersole
 */
public interface CollectionInitializer extends Initializer {
	@Override
	PluralAttributeMapping getInitializedPart();

	default CollectionPersister getInitializingCollectionDescriptor() {
		return getInitializedPart().getCollectionDescriptor();
	}

	PersistentCollection getCollectionInstance();

	@Override
	default Object getInitializedInstance() {
		return getCollectionInstance();
	}

	/**
	 * Lifecycle method called at the very end of the result values processing
	 */
	default void endLoading(ExecutionContext context) {
		// by default - nothing to do
	}

	CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState);
}
