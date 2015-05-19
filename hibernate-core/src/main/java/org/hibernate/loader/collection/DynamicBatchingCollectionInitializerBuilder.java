/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.JoinWalker;
import org.hibernate.loader.Loader;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * A BatchingCollectionInitializerBuilder that builds CollectionInitializer instances capable of dynamically building
 * its batch-fetch SQL based on the actual number of collections keys waiting to be fetched.
 *
 * @author Steve Ebersole
 */
public class DynamicBatchingCollectionInitializerBuilder extends BatchingCollectionInitializerBuilder {
	public static final DynamicBatchingCollectionInitializerBuilder INSTANCE = new DynamicBatchingCollectionInitializerBuilder();

	@Override
	protected CollectionInitializer createRealBatchingCollectionInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new DynamicBatchingCollectionInitializer( persister, maxBatchSize, factory, influencers );
	}

	@Override
	protected CollectionInitializer createRealBatchingOneToManyInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new DynamicBatchingCollectionInitializer( persister, maxBatchSize, factory, influencers );
	}

	public static class DynamicBatchingCollectionInitializer extends BatchingCollectionInitializer {
		private final int maxBatchSize;
		private final Loader singleKeyLoader;
		private final DynamicBatchingCollectionLoader batchLoader;

		public DynamicBatchingCollectionInitializer(
				QueryableCollection collectionPersister,
				int maxBatchSize,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers influencers) {
			super( collectionPersister );
			this.maxBatchSize = maxBatchSize;

			if ( collectionPersister.isOneToMany() ) {
				this.singleKeyLoader = new OneToManyLoader( collectionPersister, 1, factory, influencers );
			}
			else {
				this.singleKeyLoader = new BasicCollectionLoader( collectionPersister, 1, factory, influencers );
			}

			this.batchLoader = new DynamicBatchingCollectionLoader( collectionPersister, factory, influencers );
		}

		@Override
		public void initialize(Serializable id, SessionImplementor session) throws HibernateException {
			// first, figure out how many batchable ids we have...
			final Serializable[] batch = session.getPersistenceContext()
					.getBatchFetchQueue()
					.getCollectionBatch( collectionPersister(), id, maxBatchSize );
			final int numberOfIds = ArrayHelper.countNonNull( batch );
			if ( numberOfIds <= 1 ) {
				singleKeyLoader.loadCollection( session, id, collectionPersister().getKeyType() );
				return;
			}

			final Serializable[] idsToLoad = new Serializable[numberOfIds];
			System.arraycopy( batch, 0, idsToLoad, 0, numberOfIds );

			batchLoader.doBatchedCollectionLoad( session, idsToLoad, collectionPersister().getKeyType() );
		}
	}

	private static class DynamicBatchingCollectionLoader extends CollectionLoader {
		// todo : this represents another case where the current Loader contract is unhelpful
		//		the other recent case was stored procedure support.  Really any place where the SQL
		//		generation is dynamic but the "loading plan" remains constant.  The long term plan
		//		is to split Loader into (a) PreparedStatement generation/execution and (b) ResultSet
		// 		processing.
		//
		// Same holds true for org.hibernate.loader.entity.DynamicBatchingEntityLoaderBuilder.DynamicBatchingEntityLoader
		//
		// for now I will essentially semi-re-implement the collection loader contract here to be able to alter
		// 		the SQL (specifically to be able to dynamically build the WHERE-clause IN-condition) later, when
		//		we actually know the ids to batch fetch

		private final String sqlTemplate;
		private final String alias;

		public DynamicBatchingCollectionLoader(
				QueryableCollection collectionPersister,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers influencers) {
			super( collectionPersister, factory, influencers );

			JoinWalker walker = buildJoinWalker( collectionPersister, factory, influencers );
			initFromWalker( walker );
			this.sqlTemplate = walker.getSQLString();
			this.alias = StringHelper.generateAlias( collectionPersister.getRole(), 0 );
			postInstantiate();

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"SQL-template for dynamic collection [%s] batch-fetching : %s",
						collectionPersister.getRole(),
						sqlTemplate
				);
			}
		}

		private JoinWalker buildJoinWalker(
				QueryableCollection collectionPersister,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers influencers) {

			if ( collectionPersister.isOneToMany() ) {
				return new OneToManyJoinWalker( collectionPersister, -1, null, factory, influencers ) {
					@Override
					protected StringBuilder whereString(String alias, String[] columnNames, String subselect, int batchSize) {
						if ( subselect != null ) {
							return super.whereString( alias, columnNames, subselect, batchSize );
						}

						return StringHelper.buildBatchFetchRestrictionFragment( alias, columnNames, getFactory().getDialect() );
					}
				};
			}
			else {
				return new BasicCollectionJoinWalker( collectionPersister, -1, null, factory, influencers ) {
					@Override
					protected StringBuilder whereString(String alias, String[] columnNames, String subselect, int batchSize) {
						if ( subselect != null ) {
							return super.whereString( alias, columnNames, subselect, batchSize );
						}

						return StringHelper.buildBatchFetchRestrictionFragment( alias, columnNames, getFactory().getDialect() );
					}
				};
			}
		}

		public final void doBatchedCollectionLoad(
				final SessionImplementor session,
				final Serializable[] ids,
				final Type type) throws HibernateException {

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Batch loading collection: %s",
						MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() )
				);
			}

			final Type[] idTypes = new Type[ids.length];
			Arrays.fill( idTypes, type );
			final QueryParameters queryParameters = new QueryParameters( idTypes, ids, ids );

			final String sql = StringHelper.expandBatchIdPlaceholder(
					sqlTemplate,
					ids,
					alias,
					collectionPersister().getKeyColumnNames(),
					getFactory().getDialect()
			);

			try {
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
				if ( queryParameters.isReadOnlyInitialized() ) {
					// The read-only/modifiable mode for the query was explicitly set.
					// Temporarily set the default read-only/modifiable setting to the query's setting.
					persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
				}
				else {
					// The read-only/modifiable setting for the query was not initialized.
					// Use the default read-only/modifiable from the persistence context instead.
					queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
				}
				persistenceContext.beforeLoad();
				try {
					try {
						doTheLoad( sql, queryParameters, session );
					}
					finally {
						persistenceContext.afterLoad();
					}
					persistenceContext.initializeNonLazyCollections();
				}
				finally {
					// Restore the original default
					persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
				}
			}
			catch ( SQLException e ) {
				throw getFactory().getSQLExceptionHelper().convert(
						e,
						"could not initialize a collection batch: " +
								MessageHelper.collectionInfoString( collectionPersister(), ids, getFactory() ),
						sql
				);
			}

			LOG.debug( "Done batch load" );

		}

		private void doTheLoad(String sql, QueryParameters queryParameters, SessionImplementor session) throws SQLException {
			final RowSelection selection = queryParameters.getRowSelection();
			final int maxRows = LimitHelper.hasMaxRows( selection ) ?
					selection.getMaxRows() :
					Integer.MAX_VALUE;

			final List<AfterLoadAction> afterLoadActions = Collections.emptyList();
			final SqlStatementWrapper wrapper = executeQueryStatement( sql, queryParameters, false, afterLoadActions, session );
			final ResultSet rs = wrapper.getResultSet();
			final Statement st = wrapper.getStatement();
			try {
				processResultSet( rs, queryParameters, session, true, null, maxRows, afterLoadActions );
			}
			finally {
				session.getJdbcCoordinator().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}

	}

}
