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

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.process.spi.RowReader;
import org.hibernate.loader.plan.exec.process.spi.ScrollableResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.transform.ResultTransformer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessorImpl implements ResultSetProcessor {
	private static final Logger LOG = Logger.getLogger( ResultSetProcessorImpl.class );

	private final LoadPlan loadPlan;
	private final RowReader rowReader;

	private final boolean hadSubselectFetches;

	public ResultSetProcessorImpl(LoadPlan loadPlan, RowReader rowReader, boolean hadSubselectFetches) {
		this.loadPlan = loadPlan;
		this.rowReader = rowReader;
		this.hadSubselectFetches = hadSubselectFetches;
	}

	public RowReader getRowReader() {
		return rowReader;
	}

	@Override
	public ScrollableResultSetProcessor toOnDemandForm() {
		// todo : implement
		throw new NotYetImplementedException();
	}

	@Override
	public List extractResults(
			ResultSet resultSet,
			final SessionImplementor session,
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext,
			boolean returnProxies,
			boolean readOnly,
			ResultTransformer forcedResultTransformer,
			List<AfterLoadAction> afterLoadActionList) throws SQLException {

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

		rowReader.finishUp( context, afterLoadActionList );
		context.wrapUp();

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
			}
			session.getPersistenceContext()
					.getLoadContexts()
					.getCollectionLoadContext( resultSet )
					.getLoadingCollection( persister, key );
		}
	}


//	private class LocalVisitationStrategy extends LoadPlanVisitationStrategyAdapter {
//		private boolean hadSubselectFetches = false;
//
//		@Override
//		public void startingEntityFetch(EntityFetch entityFetch) {
//		// only collections are currently supported for subselect fetching.
//		//			hadSubselectFetches = hadSubselectFetches
//		//					|| entityFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
//		}
//
//		@Override
//		public void startingCollectionFetch(CollectionFetch collectionFetch) {
//			hadSubselectFetches = hadSubselectFetches
//					|| collectionFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
//		}
//	}
//
//	private class MixedReturnRowReader extends AbstractRowReader implements RowReader {
//		private final List<ReturnReader> returnReaders;
//		private List<EntityReferenceReader> entityReferenceReaders = new ArrayList<EntityReferenceReader>();
//		private List<CollectionReferenceReader> collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();
//
//		private final int numberOfReturns;
//
//		public MixedReturnRowReader(LoadPlan loadPlan) {
//			LoadPlanVisitor.visit(
//					loadPlan,
//					new LoadPlanVisitationStrategyAdapter() {
//						@Override
//						public void startingEntityFetch(EntityFetch entityFetch) {
//							entityReferenceReaders.add( new EntityReferenceReader( entityFetch ) );
//						}
//
//						@Override
//						public void startingCollectionFetch(CollectionFetch collectionFetch) {
//							collectionReferenceReaders.add( new CollectionReferenceReader( collectionFetch ) );
//						}
//					}
//			);
//
//			final List<ReturnReader> readers = new ArrayList<ReturnReader>();
//
//			for ( Return rtn : loadPlan.getReturns() ) {
//				final ReturnReader returnReader = buildReturnReader( rtn );
//				if ( EntityReferenceReader.class.isInstance( returnReader ) ) {
//					entityReferenceReaders.add( (EntityReferenceReader) returnReader );
//				}
//				readers.add( returnReader );
//			}
//
//			this.returnReaders = readers;
//			this.numberOfReturns = readers.size();
//		}
//
//		@Override
//		protected List<EntityReferenceReader> getEntityReferenceReaders() {
//			return entityReferenceReaders;
//		}
//
//		@Override
//		protected List<CollectionReferenceReader> getCollectionReferenceReaders() {
//			return collectionReferenceReaders;
//		}
//
//		@Override
//		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
//			Object[] logicalRow = new Object[ numberOfReturns ];
//			int pos = 0;
//			for ( ReturnReader reader : returnReaders ) {
//				logicalRow[pos] = reader.read( resultSet, context );
//				pos++;
//			}
//			return logicalRow;
//		}
//	}
//
//	private class CollectionInitializerRowReader extends AbstractRowReader implements RowReader {
//		private final CollectionReturnReader returnReader;
//
//		private List<EntityReferenceReader> entityReferenceReaders = null;
//		private final List<CollectionReferenceReader> collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();
//
//		public CollectionInitializerRowReader(LoadPlan loadPlan) {
//			returnReader = (CollectionReturnReader) buildReturnReader( loadPlan.getReturns().get( 0 ) );
//
//			LoadPlanVisitor.visit(
//					loadPlan,
//					new LoadPlanVisitationStrategyAdapter() {
//						@Override
//						public void startingEntityFetch(EntityFetch entityFetch) {
//							if ( entityReferenceReaders == null ) {
//								entityReferenceReaders = new ArrayList<EntityReferenceReader>();
//							}
//							entityReferenceReaders.add( new EntityReferenceReader( entityFetch ) );
//						}
//
//						@Override
//						public void startingCollectionFetch(CollectionFetch collectionFetch) {
//							collectionReferenceReaders.add( new CollectionReferenceReader( collectionFetch ) );
//						}
//					}
//			);
//
//			collectionReferenceReaders.add( returnReader );
//		}
//
//		@Override
//		protected List<EntityReferenceReader> getEntityReferenceReaders() {
//			return entityReferenceReaders;
//		}
//
//		@Override
//		protected List<CollectionReferenceReader> getCollectionReferenceReaders() {
//			return collectionReferenceReaders;
//		}
//
//		@Override
//		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
//			return returnReader.read( resultSet, context );
//		}
//	}
}
