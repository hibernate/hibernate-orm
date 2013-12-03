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
package org.hibernate.loader.plan.exec.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.exec.internal.FetchStats;
import org.hibernate.loader.plan.exec.internal.LoadQueryJoinAndFetchProcessor;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.exec.process.spi.CollectionReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.EntityReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;

/**
 * @author Gail Badner
 */
public abstract class AbstractLoadQueryDetails implements LoadQueryDetails {

	private final LoadPlan loadPlan;
	private final String[] keyColumnNames;
	private final Return rootReturn;
	private final LoadQueryJoinAndFetchProcessor queryProcessor;
	private String sqlStatement;
	private ResultSetProcessor resultSetProcessor;

	/**
	 * @param rootReturn The root return reference we are processing
	 * @param select The SelectStatementBuilder
	 * @param helper The Join/Fetch helper
	 * @param factory The SessionFactory
	 * @param buildingParameters The query building context
	 * @param rootAlias The table alias to use
	 * @param rootLoadable The persister
	 * @param readerCollector Collector for EntityReferenceInitializer and CollectionReferenceInitializer references
	*/
	protected AbstractLoadQueryDetails(
			LoadPlan loadPlan,
			AliasResolutionContextImpl aliasResolutionContext,
			QueryBuildingParameters buildingParameters,
			String[] keyColumnNames,
			Return rootReturn,
			SessionFactoryImplementor factory) {
		this.keyColumnNames = keyColumnNames;
		this.rootReturn = rootReturn;
		this.loadPlan = loadPlan;
		this.queryProcessor = new LoadQueryJoinAndFetchProcessor( aliasResolutionContext, buildingParameters, factory );
	}

	protected QuerySpace getQuerySpace(String querySpaceUid) {
		return loadPlan.getQuerySpaces().getQuerySpaceByUid( querySpaceUid );
	}

	@Override
	public String getSqlStatement() {
		return sqlStatement;
	}

	@Override
	public ResultSetProcessor getResultSetProcessor() {
		return resultSetProcessor;
	}

	protected final Return getRootReturn() {
		return rootReturn;
	}

	protected final AliasResolutionContext getAliasResolutionContext() {
		return queryProcessor.getAliasResolutionContext();
	}

	protected final QueryBuildingParameters getQueryBuildingParameters() {
		return queryProcessor.getQueryBuildingParameters();
	}

	protected final SessionFactoryImplementor getSessionFactory() {
		return queryProcessor.getSessionFactory();
	}
	/**
	 * Main entry point for properly handling the FROM clause and and joins and restrictions
	 *
	 */
	protected void generate() {
		// There are 2 high-level requirements to perform here:
		// 	1) Determine the SQL required to carry out the given LoadPlan (and fulfill
		// 		{@code LoadQueryDetails#getSqlStatement()}).  SelectStatementBuilder collects the ongoing efforts to
		//		build the needed SQL.
		// 	2) Determine how to read information out of the ResultSet resulting from executing the indicated SQL
		//		(the SQL aliases).  ReaderCollector and friends are where this work happens, ultimately
		//		producing a ResultSetProcessor

		final SelectStatementBuilder select = new SelectStatementBuilder( queryProcessor.getSessionFactory().getDialect() );

		// LoadPlan is broken down into 2 high-level pieces that we need to process here.
		//
		// First is the QuerySpaces, which roughly equates to the SQL FROM-clause.  We'll cycle through
		// those first, generating aliases into the AliasContext in addition to writing SQL FROM-clause information
		// into SelectStatementBuilder.  The AliasContext is populated here and the reused while process the SQL
		// SELECT-clause into the SelectStatementBuilder and then again also to build the ResultSetProcessor

		applyRootReturnTableFragments( select );

		if ( shouldApplyRootReturnFilterBeforeKeyRestriction() ) {
			applyRootReturnFilterRestrictions( select );
			// add restrictions...
			// first, the load key restrictions (which entity(s)/collection(s) do we want to load?)
			applyKeyRestriction(
					select,
					getRootTableAlias(),
					keyColumnNames,
					getQueryBuildingParameters().getBatchSize()
			);
		}
		else {
			// add restrictions...
			// first, the load key restrictions (which entity(s)/collection(s) do we want to load?)
			applyKeyRestriction(
					select,
					getRootTableAlias(),
					keyColumnNames,
					getQueryBuildingParameters().getBatchSize()
			);
			applyRootReturnFilterRestrictions( select );
		}


		applyRootReturnWhereJoinRestrictions( select );

		applyRootReturnOrderByFragments( select );
		// then move on to joins...

		applyRootReturnSelectFragments( select );

		queryProcessor.processQuerySpaceJoins( getRootQuerySpace(), select );

		// Next, we process the Returns and Fetches building the SELECT clause and at the same time building
		// Readers for reading the described results out of a SQL ResultSet

		FetchStats fetchStats = null;
		if ( FetchSource.class.isInstance( rootReturn ) ) {
			fetchStats = queryProcessor.processFetches(
					(FetchSource) rootReturn,
					select,
					getReaderCollector()
			);
		}
		else if ( CollectionReturn.class.isInstance( rootReturn ) ) {
			final CollectionReturn collectionReturn = (CollectionReturn) rootReturn;
			if ( collectionReturn.getElementGraph() != null ) {
				fetchStats = queryProcessor.processFetches(
						collectionReturn.getElementGraph(),
						select,
						getReaderCollector()
				);
			}
			// TODO: what about index???
		}

		LoadPlanTreePrinter.INSTANCE.logTree( loadPlan, queryProcessor.getAliasResolutionContext() );

		this.sqlStatement = select.toStatementString();
		this.resultSetProcessor = new ResultSetProcessorImpl(
				loadPlan,
				getReaderCollector().buildRowReader(),
				fetchStats != null && fetchStats.hasSubselectFetches()
		);
	}

	protected abstract ReaderCollector getReaderCollector();
	protected abstract QuerySpace getRootQuerySpace();
	protected abstract String getRootTableAlias();
	protected abstract boolean shouldApplyRootReturnFilterBeforeKeyRestriction();
	protected abstract void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder );
	protected abstract void applyRootReturnTableFragments(SelectStatementBuilder selectStatementBuilder);
	protected abstract void applyRootReturnFilterRestrictions(SelectStatementBuilder selectStatementBuilder);
	protected abstract void applyRootReturnWhereJoinRestrictions(SelectStatementBuilder selectStatementBuilder);
	protected abstract void applyRootReturnOrderByFragments(SelectStatementBuilder selectStatementBuilder);


		private static void applyKeyRestriction(SelectStatementBuilder select, String alias, String[] keyColumnNames, int batchSize) {
		if ( keyColumnNames.length==1 ) {
			// NOT A COMPOSITE KEY
			// 		for batching, use "foo in (?, ?, ?)" for batching
			//		for no batching, use "foo = ?"
			// (that distinction is handled inside InFragment)
			final InFragment in = new InFragment().setColumn( alias, keyColumnNames[0] );
			for ( int i = 0; i < batchSize; i++ ) {
				in.addValue( "?" );
			}
			select.appendRestrictions( in.toFragmentString() );
		}
		else {
			// A COMPOSITE KEY...
			final ConditionFragment keyRestrictionBuilder = new ConditionFragment()
					.setTableAlias( alias )
					.setCondition( keyColumnNames, "?" );
			final String keyRestrictionFragment = keyRestrictionBuilder.toFragmentString();

			StringBuilder restrictions = new StringBuilder();
			if ( batchSize==1 ) {
				// for no batching, use "foo = ? and bar = ?"
				restrictions.append( keyRestrictionFragment );
			}
			else {
				// for batching, use "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )"
				restrictions.append( '(' );
				DisjunctionFragment df = new DisjunctionFragment();
				for ( int i=0; i<batchSize; i++ ) {
					df.addCondition( keyRestrictionFragment );
				}
				restrictions.append( df.toFragmentString() );
				restrictions.append( ')' );
			}
			select.appendRestrictions( restrictions.toString() );
		}
	}

	protected abstract static class ReaderCollectorImpl implements ReaderCollector {
		private final List<EntityReferenceInitializer> entityReferenceInitializers = new ArrayList<EntityReferenceInitializer>();
		private List<CollectionReferenceInitializer> arrayReferenceInitializers;
		private List<CollectionReferenceInitializer> collectionReferenceInitializers;

		@Override
		public void add(CollectionReferenceInitializer collectionReferenceInitializer) {
			if ( collectionReferenceInitializer.getCollectionReference().getCollectionPersister().isArray() ) {
				if ( arrayReferenceInitializers == null ) {
					arrayReferenceInitializers = new ArrayList<CollectionReferenceInitializer>();
				}
				arrayReferenceInitializers.add( collectionReferenceInitializer );
			}
			else {
				if ( collectionReferenceInitializers == null ) {
					collectionReferenceInitializers = new ArrayList<CollectionReferenceInitializer>();
				}
				collectionReferenceInitializers.add( collectionReferenceInitializer );
			}
		}

		@Override
		public void add(EntityReferenceInitializer entityReferenceInitializer) {
			entityReferenceInitializers.add( entityReferenceInitializer );
		}

		public final List<EntityReferenceInitializer> getEntityReferenceInitializers() {
			return entityReferenceInitializers;
		}

		public List<CollectionReferenceInitializer> getArrayReferenceInitializers() {
			return arrayReferenceInitializers;

		}

		public List<CollectionReferenceInitializer> getNonArrayCollectionReferenceInitializers() {
			return collectionReferenceInitializers;
		}
	}
}
