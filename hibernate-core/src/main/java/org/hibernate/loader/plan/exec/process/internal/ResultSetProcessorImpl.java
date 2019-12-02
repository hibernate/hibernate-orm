/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.process.spi.RowReader;
import org.hibernate.loader.plan.exec.process.spi.ScrollableResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.ast.spi.AfterLoadAction;
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
	private final AliasResolutionContext aliasResolutionContext;
	private final RowReader rowReader;

	private final boolean hadSubselectFetches;

	private final boolean shouldUseOptionalEntityInstance;

	// There are times when the "optional entity information" on QueryParameters should be used and
	// times when they should be ignored.  Loader uses its isSingleRowLoader method to allow
	// subclasses to override that.  Collection initializers, batch loaders, e.g. override that
	// it to be false.  The 'shouldUseOptionalEntityInstance' setting is meant to fill that same role.
	public ResultSetProcessorImpl(
			LoadPlan loadPlan,
			AliasResolutionContext aliasResolutionContext,
			RowReader rowReader,
			boolean shouldUseOptionalEntityInstance,
			boolean hadSubselectFetches) {
		this.loadPlan = loadPlan;
		this.aliasResolutionContext = aliasResolutionContext;
		this.rowReader = rowReader;
		this.shouldUseOptionalEntityInstance = shouldUseOptionalEntityInstance;
		this.hadSubselectFetches = hadSubselectFetches;
	}

	@Override
	public ScrollableResultSetProcessor toOnDemandForm() {
		// todo : implement
		throw new NotYetImplementedException();
	}

	@Override
	public List extractResults(
			ResultSet resultSet,
			final SharedSessionContractImplementor session,
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext,
			boolean returnProxies,
			boolean readOnly,
			ResultTransformer forcedResultTransformer,
			List<AfterLoadAction> afterLoadActionList) throws SQLException {

		handlePotentiallyEmptyCollectionRootReturns( loadPlan, queryParameters.getCollectionKeys(), resultSet, session );

		final boolean traceEnabled = LOG.isTraceEnabled();
		final int maxRows;
		final List loadResults;
		final RowSelection selection = queryParameters.getRowSelection();
		if ( LimitHelper.hasMaxRows( selection ) ) {
			maxRows = selection.getMaxRows();
			if ( traceEnabled ) {
				LOG.tracef( "Limiting ResultSet processing to just %s rows", maxRows );
			}
			int sizeHint = maxRows < 50 ? maxRows : 50;
			loadResults = new ArrayList( sizeHint );
		}
		else {
			loadResults = new ArrayList();
			maxRows = Integer.MAX_VALUE;
		}

		final ResultSetProcessingContextImpl context = new ResultSetProcessingContextImpl(
				resultSet,
				session,
				loadPlan,
				aliasResolutionContext,
				readOnly,
				shouldUseOptionalEntityInstance,
				returnProxies,
				queryParameters,
				namedParameterContext,
				hadSubselectFetches
		);

		if ( traceEnabled ) {
			LOG.trace( "Processing result set" );
		}
		int count;
		for ( count = 0; count < maxRows && resultSet.next(); count++ ) {
			if ( traceEnabled ) {
				LOG.tracef( "Starting ResultSet row #%s", count );
			}

			Object logicalRow = rowReader.readRow( resultSet, context );

			// todo : apply transformers here?

			loadResults.add( logicalRow );

			context.finishUpRow();
		}

		if ( traceEnabled ) {
			LOG.tracev( "Done processing result set ({0} rows)", count );
		}

		rowReader.finishUp( context, afterLoadActionList );
		context.wrapUp();

		session.getPersistenceContextInternal().initializeNonLazyCollections();

		return loadResults;
	}


	private void handlePotentiallyEmptyCollectionRootReturns(
			LoadPlan loadPlan,
			Serializable[] collectionKeys,
			ResultSet resultSet,
			SharedSessionContractImplementor session) {
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
		final CollectionPersister persister = ( (CollectionReturn) loadPlan.getReturns().get( 0 ) ).getCollectionPersister();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final boolean debugEnabled = LOG.isDebugEnabled();
		for ( Serializable key : collectionKeys ) {
			if ( debugEnabled ) {
				LOG.debugf(
						"Preparing collection initializer : %s",
							MessageHelper.collectionInfoString( persister, key, session.getFactory() )
				);
			}
			persistenceContext
					.getLoadContexts()
					.findLoadingCollectionEntry( new CollectionKey( persister, key ) ).getCollectionInstance();
		}
	}

}
