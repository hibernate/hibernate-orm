/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Represents a collection currently being loaded.
 *
 * @author Steve Ebersole
 */
public interface LoadingCollectionEntry {
	/**
	 * The descriptor for the collection being loaded
	 */
	CollectionPersister getCollectionDescriptor();

	/**
	 * The initializer responsible for the loading
	 */
	CollectionInitializer<?> getInitializer();

	/**
	 * The collection key.
	 */
	Object getKey();

	/**
	 * The collection instance being loaded
	 */
	PersistentCollection<?> getCollectionInstance();

	/**
	 * Callback for row loading.  Allows delayed List creation
	 */
	void load(Consumer<List<Object>> loadingEntryConsumer);

	/**
	 * Complete the load
	 */
	void finishLoading(ExecutionContext executionContext);
}
