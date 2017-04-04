/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection.plan;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.collection.BatchingCollectionInitializerBuilder;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * Base class for LoadPlan-based BatchingCollectionInitializerBuilder implementations.  Mainly we handle the common
 * "no batching" case here to use the LoadPlan-based CollectionLoader
 *
 * @author Gail Badner
 */
public abstract class AbstractBatchingCollectionInitializerBuilder extends BatchingCollectionInitializerBuilder {

	@Override
	protected CollectionInitializer buildNonBatchingLoader(
			QueryableCollection persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return CollectionLoader.forCollection( persister ).withInfluencers( influencers ).byKey();
	}
}
