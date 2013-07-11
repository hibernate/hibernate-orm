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
package org.hibernate.loader.plan.exec.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.exec.internal.Helper;
import org.hibernate.loader.plan.exec.internal.LoadQueryBuilderHelper;
import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.EntityIdentifierReaderImpl;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.EntityReturnReader;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.exec.process.spi.AbstractRowReader;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;

import static org.hibernate.loader.plan.exec.internal.LoadQueryBuilderHelper.FetchStats;

/**
 * Handles interpreting a LoadPlan (for loading of an entity) by:<ul>
 *     <li>generating the SQL query to perform</li>
 *     <li>creating the readers needed to read the results from the SQL's ResultSet</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class EntityLoadQueryDetails implements LoadQueryDetails {
	// todo : keep around the LoadPlan?  Any benefit?
	private final LoadPlan loadPlan;

	private final String sqlStatement;
	private final ResultSetProcessor resultSetProcessor;

	/**
	 * Constructs a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param keyColumnNames The columns to load the entity by (the PK columns or some other unique set of columns)
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 *
	 * @return The EntityLoadQueryDetails
	 */
	public static EntityLoadQueryDetails makeForBatching(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		final int batchSize = buildingParameters.getBatchSize();
		final boolean shouldUseOptionalEntityInformation = batchSize == 1;

		return new EntityLoadQueryDetails(
				loadPlan,
				keyColumnNames,
				shouldUseOptionalEntityInformation,
				buildingParameters,
				factory
		);
	}

	protected EntityLoadQueryDetails(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			boolean shouldUseOptionalEntityInformation,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		this.loadPlan = loadPlan;

		final SelectStatementBuilder select = new SelectStatementBuilder( factory.getDialect() );
		final EntityReturn rootReturn = Helper.INSTANCE.extractRootReturn( loadPlan, EntityReturn.class );
		final AliasResolutionContext aliasResolutionContext = new AliasResolutionContextImpl( factory );
		final ReaderCollectorImpl readerCollector = new ReaderCollectorImpl();

		final String[] keyColumnNamesToUse = keyColumnNames != null
				? keyColumnNames
				: ( (Queryable) rootReturn.getEntityPersister() ).getIdentifierColumnNames();

		// apply root entity return specifics
		applyRootReturnSpecifics(
				select,
				keyColumnNamesToUse,
				rootReturn,
				factory,
				buildingParameters,
				aliasResolutionContext
		);
		readerCollector.addReader(
				new EntityReturnReader(
						rootReturn,
						aliasResolutionContext.resolveAliases( rootReturn ),
						new EntityIdentifierReaderImpl(
								rootReturn,
								aliasResolutionContext.resolveAliases( rootReturn ),
								Collections.<EntityReferenceReader>emptyList()
						)
				)
		);

		FetchStats fetchStats = LoadQueryBuilderHelper.applyJoinFetches(
				select,
				factory,
				rootReturn,
				buildingParameters,
				aliasResolutionContext,
				readerCollector
		);

		this.sqlStatement = select.toStatementString();
		this.resultSetProcessor = new ResultSetProcessorImpl(
				loadPlan,
				readerCollector.buildRowReader(),
				fetchStats.hasSubselectFetches()
		);
	}

	protected void applyRootReturnSpecifics(
			SelectStatementBuilder select,
			String[] keyColumnNames,
			EntityReturn rootReturn,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final String rootAlias = aliasResolutionContext.resolveAliases( rootReturn ).getTableAlias();
		final OuterJoinLoadable rootLoadable = (OuterJoinLoadable) rootReturn.getEntityPersister();
		final Queryable rootQueryable = (Queryable) rootReturn.getEntityPersister();

		applyKeyRestriction( select, rootAlias, keyColumnNames, buildingParameters.getBatchSize() );
		select.appendRestrictions(
				rootQueryable.filterFragment(
						rootAlias,
						buildingParameters.getQueryInfluencers().getEnabledFilters()
				)
		);
		select.appendRestrictions( rootLoadable.whereJoinFragment( rootAlias, true, true ) );
		select.appendSelectClauseFragment(
				rootLoadable.selectFragment(
						rootAlias,
						aliasResolutionContext.resolveAliases( rootReturn ).getColumnAliases().getSuffix()
				)
		);

		final String fromTableFragment;
		if ( buildingParameters.getLockOptions() != null ) {
			fromTableFragment = factory.getDialect().appendLockHint(
					buildingParameters.getLockOptions(),
					rootLoadable.fromTableFragment( rootAlias )
			);
			select.setLockOptions( buildingParameters.getLockOptions() );
		}
		else if ( buildingParameters.getLockMode() != null ) {
			fromTableFragment = factory.getDialect().appendLockHint(
					buildingParameters.getLockMode(),
					rootLoadable.fromTableFragment( rootAlias )
			);
			select.setLockMode( buildingParameters.getLockMode() );
		}
		else {
			fromTableFragment = rootLoadable.fromTableFragment( rootAlias );
		}
		select.appendFromClauseFragment( fromTableFragment + rootLoadable.fromJoinFragment( rootAlias, true, true ) );
	}

	private void applyKeyRestriction(SelectStatementBuilder select, String alias, String[] keyColumnNames, int batchSize) {
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

	@Override
	public String getSqlStatement() {
		return sqlStatement;
	}

	@Override
	public ResultSetProcessor getResultSetProcessor() {
		return resultSetProcessor;
	}

	private static class ReaderCollectorImpl implements ReaderCollector {
		private EntityReturnReader rootReturnReader;
		private List<EntityReferenceReader> entityReferenceReaders;
		private List<CollectionReferenceReader> collectionReferenceReaders;

		@Override
		public void addReader(CollectionReferenceReader collectionReferenceReader) {
			if ( collectionReferenceReaders == null ) {
				collectionReferenceReaders = new ArrayList<CollectionReferenceReader>();
			}
			collectionReferenceReaders.add( collectionReferenceReader );
		}

		@Override
		public void addReader(EntityReferenceReader entityReferenceReader) {
			if ( EntityReturnReader.class.isInstance( entityReferenceReader ) ) {
				if ( rootReturnReader != null ) {
					throw new IllegalStateException( "Root return reader already set" );
				}
				rootReturnReader = (EntityReturnReader) entityReferenceReader;
			}

			if ( entityReferenceReaders == null ) {
				entityReferenceReaders = new ArrayList<EntityReferenceReader>();
			}
			entityReferenceReaders.add( entityReferenceReader );
		}

		public RowReader buildRowReader() {
			return new EntityLoaderRowReader( rootReturnReader, entityReferenceReaders, collectionReferenceReaders );
		}
	}

	public static class EntityLoaderRowReader extends AbstractRowReader implements RowReader {
		private final EntityReturnReader rootReturnReader;
		private final List<EntityReferenceReader> entityReferenceReaders;
		private final List<CollectionReferenceReader> collectionReferenceReaders;

		public EntityLoaderRowReader(
				EntityReturnReader rootReturnReader,
				List<EntityReferenceReader> entityReferenceReaders,
				List<CollectionReferenceReader> collectionReferenceReaders) {
			this.rootReturnReader = rootReturnReader;
			this.entityReferenceReaders = entityReferenceReaders != null
					? entityReferenceReaders
					: Collections.<EntityReferenceReader>emptyList();
			this.collectionReferenceReaders = collectionReferenceReaders != null
					? collectionReferenceReaders
					: Collections.<CollectionReferenceReader>emptyList();
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
			return rootReturnReader.read( resultSet, context );
		}
	}
}
