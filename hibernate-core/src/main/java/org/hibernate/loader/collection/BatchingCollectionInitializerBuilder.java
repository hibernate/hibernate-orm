/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * Contract for building {@link CollectionInitializer} instances capable of performing batch-fetch loading.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.BatchFetchStyle
 */
public abstract class BatchingCollectionInitializerBuilder {
	public static BatchingCollectionInitializerBuilder getBuilder(SessionFactoryImplementor factory) {
		switch ( factory.getSettings().getBatchFetchStyle() ) {
			case PADDED: {
				return PaddedBatchingCollectionInitializerBuilder.INSTANCE;
			}
			case DYNAMIC: {
				return DynamicBatchingCollectionInitializerBuilder.INSTANCE;
			}
			default: {
				return org.hibernate.loader.collection.plan.LegacyBatchingCollectionInitializerBuilder.INSTANCE;
				//return LegacyBatchingCollectionInitializerBuilder.INSTANCE;
			}
		}
	}

	/**
	 * Builds a batch-fetch capable CollectionInitializer for basic and many-to-many collections (collections with
	 * a dedicated collection table).
	 *
	 * @param persister THe collection persister
	 * @param maxBatchSize The maximum number of keys to batch-fetch together
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 *
	 * @return The batch-fetch capable collection initializer
	 */
	public CollectionInitializer createBatchingCollectionInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if ( maxBatchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, factory, influencers );
		}

		return createRealBatchingCollectionInitializer( persister, maxBatchSize, factory, influencers );
	}

	protected abstract CollectionInitializer createRealBatchingCollectionInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers);


	/**
	 * Builds a batch-fetch capable CollectionInitializer for one-to-many collections (collections without
	 * a dedicated collection table).
	 *
	 * @param persister THe collection persister
	 * @param maxBatchSize The maximum number of keys to batch-fetch together
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 *
	 * @return The batch-fetch capable collection initializer
	 */
	public CollectionInitializer createBatchingOneToManyInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if ( maxBatchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, factory, influencers );
		}

		return createRealBatchingOneToManyInitializer( persister, maxBatchSize, factory, influencers );
	}

	protected abstract CollectionInitializer createRealBatchingOneToManyInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers);

	protected CollectionInitializer buildNonBatchingLoader(
			QueryableCollection persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return persister.isOneToMany() ?
				new OneToManyLoader( persister, factory, influencers ) :
				new BasicCollectionLoader( persister, factory, influencers );
	}
}
