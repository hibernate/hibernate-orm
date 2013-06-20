/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.plan.exec.process.spi.ReturnReader;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.ScalarReturn;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.visit.LoadPlanVisitor;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.loader.spi.LoadPlanAdvisor;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.process.spi.ScrollableResultSetProcessor;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.transform.ResultTransformer;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessorImpl implements ResultSetProcessor {
	private static final Logger LOG = Logger.getLogger( ResultSetProcessorImpl.class );

	private final LoadPlan baseLoadPlan;
	private final RowReader rowReader;

	private final boolean shouldUseOptionalEntityInstance;

	private final boolean hadSubselectFetches;

	public ResultSetProcessorImpl(
			LoadPlan loadPlan,
			boolean shouldUseOptionalEntityInstance) {
		this.baseLoadPlan = loadPlan;
		this.rowReader = buildRowReader( loadPlan );
		this.shouldUseOptionalEntityInstance = shouldUseOptionalEntityInstance;

		LocalVisitationStrategy strategy = new LocalVisitationStrategy();
		LoadPlanVisitor.visit( loadPlan, strategy );
		this.hadSubselectFetches = strategy.hadSubselectFetches;
	}

	private RowReader buildRowReader(LoadPlan loadPlan) {
		switch ( loadPlan.getDisposition() ) {
			case MIXED: {
				return new MixedReturnRowReader( loadPlan );
			}
			case ENTITY_LOADER: {
				return new EntityLoaderRowReader( loadPlan );
			}
			case COLLECTION_INITIALIZER: {
				return new CollectionInitializerRowReader( loadPlan );
			}
			default: {
				throw new IllegalStateException( "Unrecognized LoadPlan Return dispostion : " + loadPlan.getDisposition() );
			}
		}
	}

	@Override
	public ScrollableResultSetProcessor toOnDemandForm() {
		// todo : implement
		throw new NotYetImplementedException();
	}

	@Override
	public List extractResults(
			LoadPlanAdvisor loadPlanAdvisor,
			ResultSet resultSet,
			final SessionImplementor session,
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext,
			AliasResolutionContext aliasResolutionContext,
			boolean returnProxies,
			boolean readOnly,
			ResultTransformer forcedResultTransformer,
			List<AfterLoadAction> afterLoadActionList) throws SQLException {

		final LoadPlan loadPlan = loadPlanAdvisor.advise( this.baseLoadPlan );
		if ( loadPlan == null ) {
			throw new IllegalStateException( "LoadPlanAdvisor returned null" );
		}

		handlePotentiallyEmptyCollectionRootReturns( loadPlan, queryParameters.getCollectionKeys(), resultSet, session );

		final int maxRows;
		final RowSelection selection = queryParameters.getRowSelection();
		if ( LimitHelper.hasMaxRows( selection ) ) {
			maxRows = selection.getMaxRows();
			LOG.tracef( "Limiting ResultSet processing to just %s rows", maxRows );
		}
		else {
			maxRows = Integer.MAX_VALUE;
		}

		// There are times when the "optional entity information" on QueryParameters should be used and
		// times when they should be ignored.  Loader uses its isSingleRowLoader method to allow
		// subclasses to override that.  Collection initializers, batch loaders, e.g. override that
		// it to be false.  The 'shouldUseOptionalEntityInstance' setting is meant to fill that same role.
		final boolean shouldUseOptionalEntityInstance = true;

		// Handles the "FETCH ALL PROPERTIES" directive in HQL
		final boolean forceFetchLazyAttributes = false;

		final ResultSetProcessingContextImpl context = new ResultSetProcessingContextImpl(
				resultSet,
				session,
				loadPlan,
				readOnly,
				shouldUseOptionalEntityInstance,
				forceFetchLazyAttributes,
				returnProxies,
				queryParameters,
				namedParameterContext,
				aliasResolutionContext,
				hadSubselectFetches
		);

		final List loadResults = new ArrayList();

		LOG.trace( "Processing result set" );
		int count;
		for ( count = 0; count < maxRows && resultSet.next(); count++ ) {
			LOG.debugf( "Starting ResultSet row #%s", count );

			Object logicalRow = rowReader.readRow( resultSet, context );

			// todo : apply transformers here?

			loadResults.add( logicalRow );

			context.finishUpRow();
		}

		LOG.tracev( "Done processing result set ({0} rows)", count );

		context.finishUp( afterLoadActionList );

		session.getPersistenceContext().initializeNonLazyCollections();

		return loadResults;
	}


	private void handlePotentiallyEmptyCollectionRootReturns(
			LoadPlan loadPlan,
			Serializable[] collectionKeys,
			ResultSet resultSet,
			SessionImplementor session) {
		if ( collectionKeys == null ) {
			// this is not a collection initializer (and empty collections will be detected by looking for
			// the owner's identifier in the result set)
			return;
		}

		// this is a collection initializer, so we must create a collection
		// for each of the passed-in keys, to account for the possibility
		// that the collection is empty and has no rows in the result set
		//
		// todo : move this inside CollectionReturn ?
		CollectionPersister persister = ( (CollectionReturn) loadPlan.getReturns().get( 0 ) ).getCollectionPersister();
		for ( Serializable key : collectionKeys ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Preparing collection intializer : %s",
							MessageHelper.collectionInfoString( persister, key, session.getFactory() )
				);
				session.getPersistenceContext()
						.getLoadContexts()
						.getCollectionLoadContext( resultSet )
						.getLoadingCollection( persister, key );
			}
		}
	}


	private class LocalVisitationStrategy extends LoadPlanVisitationStrategyAdapter {
		private boolean hadSubselectFetches = false;

		@Override
		public void startingEntityFetch(EntityFetch entityFetch) {
		// only collections are currently supported for subselect fetching.
		//			hadSubselectFetches = hadSubselectFetches
		//					|| entityFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			hadSubselectFetches = hadSubselectFetches
					|| collectionFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
		}
	}

	private static interface RowReader {
		Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;
	}

	private static abstract class AbstractRowReader implements RowReader {

		@Override
		public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			final List<EntityReferenceReader> entityReferenceReaders = getEntityReferenceReaders();
			final List<CollectionReferenceReader> collectionReferenceReaders = getCollectionReferenceReaders();

			final boolean hasEntityReferenceReaders = entityReferenceReaders != null && entityReferenceReaders.size() > 0;
			final boolean hasCollectionReferenceReaders = collectionReferenceReaders != null && collectionReferenceReaders.size() > 0;

			if ( hasEntityReferenceReaders ) {
				// 	1) allow entity references to resolve identifiers (in 2 steps)
				for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
					entityReferenceReader.hydrateIdentifier( resultSet, context );
				}
				for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
					entityReferenceReader.resolveEntityKey( resultSet, context );
				}


				// 2) allow entity references to resolve their hydrated state and entity instance
				for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
					entityReferenceReader.hydrateEntityState( resultSet, context );
				}
			}


			// 3) read the logical row

			Object logicalRow = readLogicalRow( resultSet, context );


			// 4) allow entities and collection to read their elements
			if ( hasEntityReferenceReaders ) {
				for ( EntityReferenceReader entityReferenceReader : entityReferenceReaders ) {
					entityReferenceReader.finishUpRow( resultSet, context );
				}
			}
			if ( hasCollectionReferenceReaders ) {
				for ( CollectionReferenceReader collectionReferenceReader : collectionReferenceReaders ) {
					collectionReferenceReader.finishUpRow( resultSet, context );
				}
			}

			return logicalRow;
		}

		protected abstract List<EntityReferenceReader> getEntityReferenceReaders();
		protected abstract List<CollectionReferenceReader> getCollectionReferenceReaders();

		protected abstract Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context)
				throws SQLException;

	}

	private class MixedReturnRowReader extends AbstractRowReader implements RowReader {
		private final List<ReturnReader> returnReaders;
		private List<EntityReferenceReader> entityReferenceReaders = new ArrayList<EntityReferenceReader>();
		private List<CollectionReferenceReader> collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();

		private final int numberOfReturns;

		public MixedReturnRowReader(LoadPlan loadPlan) {
			LoadPlanVisitor.visit(
					loadPlan,
					new LoadPlanVisitationStrategyAdapter() {
						@Override
						public void startingEntityFetch(EntityFetch entityFetch) {
							entityReferenceReaders.add( new EntityReferenceReader( entityFetch ) );
						}

						@Override
						public void startingCollectionFetch(CollectionFetch collectionFetch) {
							collectionReferenceReaders.add( new CollectionReferenceReader( collectionFetch ) );
						}
					}
			);

			final List<ReturnReader> readers = new ArrayList<ReturnReader>();

			for ( Return rtn : loadPlan.getReturns() ) {
				final ReturnReader returnReader = buildReturnReader( rtn );
				if ( EntityReferenceReader.class.isInstance( returnReader ) ) {
					entityReferenceReaders.add( (EntityReferenceReader) returnReader );
				}
				readers.add( returnReader );
			}

			this.returnReaders = readers;
			this.numberOfReturns = readers.size();
		}

		@Override
		protected List<EntityReferenceReader> getEntityReferenceReaders() {
			return entityReferenceReaders;
		}

		@Override
		protected List<CollectionReferenceReader> getCollectionReferenceReaders() {
			return collectionReferenceReaders;
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			Object[] logicalRow = new Object[ numberOfReturns ];
			int pos = 0;
			for ( ReturnReader reader : returnReaders ) {
				logicalRow[pos] = reader.read( resultSet, context );
				pos++;
			}
			return logicalRow;
		}
	}

	private static ReturnReader buildReturnReader(Return rtn) {
		if ( ScalarReturn.class.isInstance( rtn ) ) {
			return new ScalarReturnReader( (ScalarReturn) rtn );
		}
		else if ( EntityReturn.class.isInstance( rtn ) ) {
			return new EntityReturnReader( (EntityReturn) rtn );
		}
		else if ( CollectionReturn.class.isInstance( rtn ) ) {
			return new CollectionReturnReader( (CollectionReturn) rtn );
		}
		else {
			throw new IllegalStateException( "Unknown Return type : " + rtn );
		}
	}

	private static interface EntityReferenceReaderListBuildingAccess {
		public void add(EntityReferenceReader reader);
	}

	private static interface CollectionReferenceReaderListBuildingAccess {
		public void add(CollectionReferenceReader reader);
	}


	private class EntityLoaderRowReader extends AbstractRowReader implements RowReader {
		private final EntityReturnReader returnReader;
		private final List<EntityReferenceReader> entityReferenceReaders = new ArrayList<EntityReferenceReader>();
		private List<CollectionReferenceReader> collectionReferenceReaders = null;

		public EntityLoaderRowReader(LoadPlan loadPlan) {
			final EntityReturn entityReturn = (EntityReturn) loadPlan.getReturns().get( 0 );
			this.returnReader = (EntityReturnReader) buildReturnReader( entityReturn );

//			final EntityReferenceReaderListBuildingAccess entityReaders = new EntityReferenceReaderListBuildingAccess() {
//				@Override
//				public void add(EntityReferenceReader reader) {
//					entityReferenceReaders.add( reader );
//				}
//			};
//
//			final CollectionReferenceReaderListBuildingAccess collectionReaders = new CollectionReferenceReaderListBuildingAccess() {
//				@Override
//				public void add(CollectionReferenceReader reader) {
//					if ( collectionReferenceReaders == null ) {
//						collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();
//					}
//					collectionReferenceReaders.add( reader );
//				}
//			};
//
//			buildFetchReaders( entityReaders, collectionReaders, entityReturn, returnReader );

			LoadPlanVisitor.visit(
					loadPlan,
					new LoadPlanVisitationStrategyAdapter() {
						@Override
						public void startingEntityFetch(EntityFetch entityFetch) {
							entityReferenceReaders.add( new EntityReferenceReader( entityFetch ) );
						}

						@Override
						public void startingCollectionFetch(CollectionFetch collectionFetch) {
							if ( collectionReferenceReaders == null ) {
								collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();
							}
							collectionReferenceReaders.add( new CollectionReferenceReader( collectionFetch ) );
						}
					}
			);

			entityReferenceReaders.add( returnReader );
		}

//		private void buildFetchReaders(
//				EntityReferenceReaderListBuildingAccess entityReaders,
//				CollectionReferenceReaderListBuildingAccess collectionReaders,
//				FetchOwner fetchOwner,
//				EntityReferenceReader entityReferenceReader) {
//			for ( Fetch fetch : fetchOwner.getFetches() ) {
//				if ( CollectionFetch.class.isInstance( fetch ) ) {
//					final CollectionFetch collectionFetch = (CollectionFetch) fetch;
//					buildFetchReaders(
//							entityReaders,
//							collectionReaders,
//							collectionFetch.getIndexGraph(),
//							null
//					);
//					buildFetchReaders(
//							entityReaders,
//							collectionReaders,
//							collectionFetch.getElementGraph(),
//							null
//					);
//					collectionReaders.add( new CollectionReferenceReader( collectionFetch ) );
//				}
//				else if ( CompositeFetch.class.isInstance( fetch ) ) {
//					buildFetchReaders(
//							entityReaders,
//							collectionReaders,
//							(CompositeFetch) fetch,
//							entityReferenceReader
//					);
//				}
//				else {
//					final EntityFetch entityFetch = (EntityFetch) fetch;
//					if ( entityFetch.getFetchedType().isOneToOne() ) {
//						// entityReferenceReader should reference the owner still...
//						if ( entityReferenceReader == null ) {
//							throw new IllegalStateException( "Entity reader for one-to-one fetch owner not known" );
//						}
//						final EntityReferenceReader fetchReader = new OneToOneFetchReader(
//								entityFetch,
//								entityReferenceReader.getEntityReference()
//						);
//					}
//					else {
//
//					}
//				}
//			}
//			//To change body of created methods use File | Settings | File Templates.
//		}

		@Override
		protected List<EntityReferenceReader> getEntityReferenceReaders() {
			return entityReferenceReaders;
		}

		@Override
		protected List<CollectionReferenceReader> getCollectionReferenceReaders() {
			return collectionReferenceReaders;
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			return returnReader.read( resultSet, context );
		}
	}

	private class CollectionInitializerRowReader extends AbstractRowReader implements RowReader {
		private final CollectionReturnReader returnReader;

		private List<EntityReferenceReader> entityReferenceReaders = null;
		private final List<CollectionReferenceReader> collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();

		public CollectionInitializerRowReader(LoadPlan loadPlan) {
			returnReader = (CollectionReturnReader) buildReturnReader( loadPlan.getReturns().get( 0 ) );

			LoadPlanVisitor.visit(
					loadPlan,
					new LoadPlanVisitationStrategyAdapter() {
						@Override
						public void startingEntityFetch(EntityFetch entityFetch) {
							if ( entityReferenceReaders == null ) {
								entityReferenceReaders = new ArrayList<EntityReferenceReader>();
							}
							entityReferenceReaders.add( new EntityReferenceReader( entityFetch ) );
						}

						@Override
						public void startingCollectionFetch(CollectionFetch collectionFetch) {
							collectionReferenceReaders.add( new CollectionReferenceReader( collectionFetch ) );
						}
					}
			);

			collectionReferenceReaders.add( returnReader );
		}

		@Override
		protected List<EntityReferenceReader> getEntityReferenceReaders() {
			return entityReferenceReaders;
		}

		@Override
		protected List<CollectionReferenceReader> getCollectionReferenceReaders() {
			return collectionReferenceReaders;
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			return returnReader.read( resultSet, context );
		}
	}
}
