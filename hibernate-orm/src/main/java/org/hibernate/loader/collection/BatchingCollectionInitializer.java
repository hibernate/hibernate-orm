/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * The base contract for loaders capable of performing batch-fetch loading of collections using multiple foreign key
 * values in the SQL <tt>WHERE</tt> clause.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see BatchingCollectionInitializerBuilder
 * @see BasicCollectionLoader
 * @see OneToManyLoader
 */
public abstract class BatchingCollectionInitializer implements CollectionInitializer {
	private final QueryableCollection collectionPersister;

	public BatchingCollectionInitializer(QueryableCollection collectionPersister) {
		this.collectionPersister = collectionPersister;
	}

	public CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public QueryableCollection collectionPersister() {
		return collectionPersister;
	}
}
