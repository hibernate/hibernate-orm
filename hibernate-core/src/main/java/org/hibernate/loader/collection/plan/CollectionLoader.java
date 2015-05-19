/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection.plan;

import java.sql.ResultSet;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Superclass for loaders that initialize collections
 * 
 * @see org.hibernate.loader.collection.OneToManyLoader
 * @see org.hibernate.loader.collection.BasicCollectionLoader
 * @author Gavin King
 * @author Gail Badner
 */
public class CollectionLoader extends AbstractLoadPlanBasedCollectionInitializer {
	private static final Logger log = CoreLogging.logger( CollectionLoader.class );

	public static Builder forCollection(QueryableCollection collectionPersister) {
		return new Builder( collectionPersister );
	}

	@Override
	protected int[] getNamedParameterLocs(String name) {
		return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	protected void autoDiscoverTypes(ResultSet rs) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	protected static class Builder {
		private final QueryableCollection collectionPersister;
		private int batchSize = 1;
		private LoadQueryInfluencers influencers = LoadQueryInfluencers.NONE;

		private Builder(QueryableCollection collectionPersister) {
			this.collectionPersister = collectionPersister;
		}

		public Builder withBatchSize(int batchSize) {
			this.batchSize = batchSize;
			return this;
		}

		public Builder withInfluencers(LoadQueryInfluencers influencers) {
			this.influencers = influencers;
			return this;
		}

		public CollectionLoader byKey() {
			final QueryBuildingParameters buildingParameters = new QueryBuildingParameters() {
				@Override
				public LoadQueryInfluencers getQueryInfluencers() {
					return influencers;
				}

				@Override
				public int getBatchSize() {
					return batchSize;
				}

				@Override
				public LockMode getLockMode() {
					return LockMode.NONE;
				}

				@Override
				public LockOptions getLockOptions() {
					return null;
				}
			};
			return new CollectionLoader( collectionPersister, buildingParameters ) ;
		}
	}

	public CollectionLoader(
			QueryableCollection collectionPersister,
			QueryBuildingParameters buildingParameters) {
		super( collectionPersister, buildingParameters );
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Static select for collection %s: %s",
					collectionPersister.getRole(),
					getStaticLoadQuery().getSqlStatement()
			);
		}
	}

	protected Type getKeyType() {
		return collectionPersister().getKeyType();
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister().getRole() + ')';
	}
}
