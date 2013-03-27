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
package org.hibernate.loader.internal;

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
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.LoadPlanVisitor;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.loader.spi.NamedParameterContext;
import org.hibernate.loader.spi.ScrollableResultSetProcessor;
import org.hibernate.loader.spi.ResultSetProcessor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.transform.ResultTransformer;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessorImpl implements ResultSetProcessor {
	private static final Logger LOG = Logger.getLogger( ResultSetProcessorImpl.class );

	private final LoadPlan loadPlan;

	private final boolean hadSubselectFetches;

	public ResultSetProcessorImpl(LoadPlan loadPlan) {
		this.loadPlan = loadPlan;

		LocalVisitationStrategy strategy = new LocalVisitationStrategy();
		LoadPlanVisitor.visit( loadPlan, strategy );
		this.hadSubselectFetches = strategy.hadSubselectFetches;
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

		handlePotentiallyEmptyCollectionRootReturns( queryParameters.getCollectionKeys(), resultSet, session );

		final int maxRows;
		final RowSelection selection = queryParameters.getRowSelection();
		if ( LimitHelper.hasMaxRows( selection ) ) {
			maxRows = selection.getMaxRows();
			LOG.tracef( "Limiting ResultSet processing to just %s rows", maxRows );
		}
		else {
			maxRows = Integer.MAX_VALUE;
		}

		final ResultSetProcessingContextImpl context = new ResultSetProcessingContextImpl(
				resultSet,
				session,
				loadPlan,
				readOnly,
//				true, // use optional entity key?  for now, always say yes
				false, // use optional entity key?  actually for now always say no since in the simple test cases true causes failures because there is no optional key
				queryParameters,
				namedParameterContext,
				hadSubselectFetches
		);

		final List loadResults = new ArrayList();

		final int rootReturnCount = loadPlan.getReturns().size();

		LOG.trace( "Processing result set" );
		int count;
		for ( count = 0; count < maxRows && resultSet.next(); count++ ) {
			LOG.debugf( "Starting ResultSet row #%s", count );

			Object logicalRow;
			if ( rootReturnCount == 1 ) {
				loadPlan.getReturns().get( 0 ).hydrate( resultSet, context );
				loadPlan.getReturns().get( 0 ).resolve( resultSet, context );

				logicalRow = loadPlan.getReturns().get( 0 ).read( resultSet, context );
			}
			else {
				for ( Return rootReturn : loadPlan.getReturns() ) {
					rootReturn.hydrate( resultSet, context );
				}
				for ( Return rootReturn : loadPlan.getReturns() ) {
					rootReturn.resolve( resultSet, context );
				}

				logicalRow = new Object[ rootReturnCount ];
				int pos = 0;
				for ( Return rootReturn : loadPlan.getReturns() ) {
					( (Object[]) logicalRow )[pos] = rootReturn.read( resultSet, context );
					pos++;
				}
			}

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
//					| entityFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			hadSubselectFetches = hadSubselectFetches
					| collectionFetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT;
		}
	}
}
