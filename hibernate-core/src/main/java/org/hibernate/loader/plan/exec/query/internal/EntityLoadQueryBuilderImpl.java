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
package org.hibernate.loader.plan.exec.query.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.internal.Helper;
import org.hibernate.loader.plan.exec.internal.LoadQueryBuilderHelper;
import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceReader;
import org.hibernate.loader.plan.exec.query.spi.EntityLoadQueryBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.spi.RowReader;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;

/**
 * @author Steve Ebersole
 */
public class EntityLoadQueryBuilderImpl implements EntityLoadQueryBuilder {
	/**
	 * Singleton access
	 */
	public static final EntityLoadQueryBuilderImpl INSTANCE = new EntityLoadQueryBuilderImpl();

	@Override
	public String generateSql(
			LoadPlan loadPlan,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final EntityReturn rootReturn = Helper.INSTANCE.extractRootReturn( loadPlan, EntityReturn.class );

		return generateSql(
				( (Queryable) rootReturn.getEntityPersister() ).getKeyColumnNames(),
				rootReturn,
				factory,
				buildingParameters,
				aliasResolutionContext
		);
	}

	@Override
	public String generateSql(
			String[] keyColumnNames,
			LoadPlan loadPlan,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final EntityReturn rootReturn = Helper.INSTANCE.extractRootReturn( loadPlan, EntityReturn.class );

		final String[] keyColumnNamesToUse = keyColumnNames != null
				? keyColumnNames
				: ( (Queryable) rootReturn.getEntityPersister() ).getIdentifierColumnNames();

		return generateSql(
				keyColumnNamesToUse,
				rootReturn,
				factory,
				buildingParameters,
				aliasResolutionContext
		);
	}

	protected String generateSql(
			String[] keyColumnNames,
			EntityReturn rootReturn,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final SelectStatementBuilder select = new SelectStatementBuilder( factory.getDialect() );

		// apply root entity return specifics
		applyRootReturnSpecifics(
				select,
				keyColumnNames,
				rootReturn,
				factory,
				buildingParameters,
				aliasResolutionContext
		);

		LoadQueryBuilderHelper.applyJoinFetches(
				select,
				factory,
				rootReturn,
				buildingParameters,
				aliasResolutionContext,
				new ReaderCollector() {

					@Override
					public void addReader(CollectionReferenceReader collectionReferenceReader) {
					}

					@Override
					public void addReader(EntityReferenceReader entityReferenceReader) {
					}
				}
		);

		return select.toStatementString();
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
}
