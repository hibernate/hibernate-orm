/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2012, Red Hat Inc. or third-party contributors as
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
