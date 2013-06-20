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

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.spi.AnyFetch;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeElementGraph;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.CompositeIndexGraph;
import org.hibernate.loader.plan.spi.EntityElementGraph;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;

/**
 * Helper for implementors of entity and collection based query building based on LoadPlans providing common
 * functionality
 *
 * @author Steve Ebersole
 */
public class LoadQueryBuilderHelper {
	private LoadQueryBuilderHelper() {
	}

	public static void applyJoinFetches(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			FetchOwner fetchOwner,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final JoinFragment joinFragment = factory.getDialect().createOuterJoinFragment();

		processJoinFetches(
				selectStatementBuilder,
				factory,
				joinFragment,
				fetchOwner,
				buildingParameters,
				aliasResolutionContext
		);

		selectStatementBuilder.setOuterJoins(
				joinFragment.toFromFragmentString(),
				joinFragment.toWhereFragmentString()
		);
	}

	private static void processJoinFetches(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		for ( Fetch fetch : fetchOwner.getFetches() ) {
			processFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					fetch,
					buildingParameters,
					aliasResolutionContext
			);
		}
	}

	private static void processFetch(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			Fetch fetch,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		if ( ! FetchStrategyHelper.isJoinFetched( fetch.getFetchStrategy() ) ) {
			return;
		}

		if ( EntityFetch.class.isInstance( fetch ) ) {
			processEntityFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					(EntityFetch) fetch,
					buildingParameters,
					aliasResolutionContext
			);
			processJoinFetches(
					selectStatementBuilder,
					factory,
					joinFragment,
					(EntityFetch) fetch,
					buildingParameters,
					aliasResolutionContext
			);
		}
		else if ( CollectionFetch.class.isInstance( fetch ) ) {
			processCollectionFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					(CollectionFetch) fetch,
					buildingParameters,
					aliasResolutionContext
			);
			final CollectionFetch collectionFetch = (CollectionFetch) fetch;
			if ( collectionFetch.getIndexGraph() != null ) {
				processJoinFetches(
						selectStatementBuilder,
						factory,
						joinFragment,
						collectionFetch.getIndexGraph(),
						buildingParameters,
						aliasResolutionContext
				);
			}
			if ( collectionFetch.getElementGraph() != null ) {
				processJoinFetches(
						selectStatementBuilder,
						factory,
						joinFragment,
						collectionFetch.getElementGraph(),
						buildingParameters,
						aliasResolutionContext
				);
			}
		}
		else {
			// could also be a CompositeFetch, we ignore those here
			// but do still need to visit their fetches...
			if ( FetchOwner.class.isInstance( fetch ) ) {
				processJoinFetches(
						selectStatementBuilder,
						factory,
						joinFragment,
						(FetchOwner) fetch,
						buildingParameters,
						aliasResolutionContext
				);
			}
		}
	}

	private static void processEntityFetch(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			EntityFetch fetch,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final String rhsAlias = aliasResolutionContext.resolveAliases( fetch ).getTableAlias();
		final String[] rhsColumnNames = JoinHelper.getRHSColumnNames( fetch.getFetchedType(), factory );

		final String lhsTableAlias = resolveLhsTableAlias( fetchOwner, fetch, aliasResolutionContext );
		// todo : this is not exactly correct.  it assumes the join refers to the LHS PK
		final String[] aliasedLhsColumnNames = fetch.toSqlSelectFragments( lhsTableAlias );

		final String additionalJoinConditions = resolveAdditionalJoinCondition(
				factory,
				rhsAlias,
				fetchOwner,
				fetch,
				buildingParameters.getQueryInfluencers(),
				aliasResolutionContext
		);

		final Joinable joinable = (Joinable) fetch.getEntityPersister();

		addJoins(
				joinFragment,
				joinable,
				fetch.isNullable() ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN,
				rhsAlias,
				rhsColumnNames,
				aliasedLhsColumnNames,
				additionalJoinConditions
		);

		// the null arguments here relate to many-to-many fetches
		selectStatementBuilder.appendSelectClauseFragment(
				joinable.selectFragment(
						null,
						null,
						rhsAlias,
						aliasResolutionContext.resolveAliases( fetch ).getColumnAliases().getSuffix(),
						null,
						true
				)
		);
	}

	private static String[] resolveAliasedLhsJoinColumns(
			FetchOwner fetchOwner,
			Fetch fetch,
			AliasResolutionContext aliasResolutionContext) {
		// IMPL NOTE : the fetch-owner is the LHS; the fetch is the RHS
		final String lhsTableAlias = resolveLhsTableAlias( fetchOwner, fetch, aliasResolutionContext );
		return fetch.toSqlSelectFragments( lhsTableAlias );
	}

	private static String resolveLhsTableAlias(
			FetchOwner fetchOwner,
			Fetch fetch,
			AliasResolutionContext aliasResolutionContext) {
		// IMPL NOTE : the fetch-owner is the LHS; the fetch is the RHS

		if ( AnyFetch.class.isInstance( fetchOwner ) ) {
			throw new WalkingException( "Any type should never be joined!" );
		}
		else if ( EntityReference.class.isInstance( fetchOwner ) ) {
			return aliasResolutionContext.resolveAliases( (EntityReference) fetchOwner ).getTableAlias();
		}
		else if ( CompositeFetch.class.isInstance( fetchOwner ) ) {
			return aliasResolutionContext.resolveAliases(
					locateCompositeFetchEntityReferenceSource( (CompositeFetch) fetchOwner )
			).getTableAlias();
		}
		else if ( CompositeElementGraph.class.isInstance( fetchOwner ) ) {
			final CompositeElementGraph compositeElementGraph = (CompositeElementGraph) fetchOwner;
			return aliasResolutionContext.resolveAliases( compositeElementGraph.getCollectionReference() ).getCollectionTableAlias();
		}
		else if ( CompositeIndexGraph.class.isInstance( fetchOwner ) ) {
			final CompositeIndexGraph compositeIndexGraph = (CompositeIndexGraph) fetchOwner;
			return aliasResolutionContext.resolveAliases( compositeIndexGraph.getCollectionReference() ).getCollectionTableAlias();
		}
		else {
			throw new NotYetImplementedException( "Cannot determine LHS alias for FetchOwner." );
		}
	}

	private static EntityReference locateCompositeFetchEntityReferenceSource(CompositeFetch composite) {
		final FetchOwner owner = composite.getOwner();
		if ( EntityReference.class.isInstance( owner ) ) {
			return (EntityReference) owner;
		}
		if ( CompositeFetch.class.isInstance( owner ) ) {
			return locateCompositeFetchEntityReferenceSource( (CompositeFetch) owner );
		}

		throw new WalkingException( "Cannot resolve entity source for a CompositeFetch" );
	}

	private static String resolveAdditionalJoinCondition(
			SessionFactoryImplementor factory,
			String rhsTableAlias,
			FetchOwner fetchOwner,
			Fetch fetch,
			LoadQueryInfluencers influencers,
			AliasResolutionContext aliasResolutionContext) {
		final String withClause = StringHelper.isEmpty( fetch.getAdditionalJoinConditions() )
				? ""
				: " and ( " + fetch.getAdditionalJoinConditions() + " )";
		return ( (AssociationType) fetch.getFetchedType() ).getOnCondition(
				rhsTableAlias,
				factory,
				influencers.getEnabledFilters()
		) + withClause;
	}

	private static void addJoins(
			JoinFragment joinFragment,
			Joinable joinable,
			JoinType joinType,
			String rhsAlias,
			String[] rhsColumnNames,
			String[] aliasedLhsColumnNames,
			String additionalJoinConditions) {
		joinFragment.addJoin(
				joinable.getTableName(),
				rhsAlias,
				aliasedLhsColumnNames,
				rhsColumnNames,
				joinType,
				additionalJoinConditions
		);
		joinFragment.addJoins(
				joinable.fromJoinFragment( rhsAlias, false, true ),
				joinable.whereJoinFragment( rhsAlias, false, true )
		);
	}

	private static void processCollectionFetch(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			CollectionFetch fetch,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final QueryableCollection queryableCollection = (QueryableCollection) fetch.getCollectionPersister();
		final Joinable joinableCollection = (Joinable) fetch.getCollectionPersister();

		if ( fetch.getCollectionPersister().isManyToMany() ) {
			// for many-to-many we have 3 table aliases.  By way of example, consider a normal m-n: User<->Role
			// where User is the FetchOwner and Role (User.roles) is the Fetch.  We'd have:
			//		1) the owner's table : user
			final String ownerTableAlias = resolveLhsTableAlias( fetchOwner, fetch, aliasResolutionContext );
			//		2) the m-n table : user_role
			final String collectionTableAlias = aliasResolutionContext.resolveAliases( fetch ).getCollectionTableAlias();
			//		3) the element table : role
			final String elementTableAlias = aliasResolutionContext.resolveAliases( fetch ).getElementTableAlias();

			{
				// add join fragments from the owner table -> collection table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				final String filterFragment = ( (Joinable) fetch.getCollectionPersister() ).filterFragment(
						collectionTableAlias,
						buildingParameters.getQueryInfluencers().getEnabledFilters()
				);

				joinFragment.addJoin(
						joinableCollection.getTableName(),
						collectionTableAlias,
						StringHelper.qualify( ownerTableAlias, extractJoinable( fetchOwner ).getKeyColumnNames() ),
						queryableCollection.getKeyColumnNames(),
						fetch.isNullable() ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN,
						filterFragment
				);
				joinFragment.addJoins(
						joinableCollection.fromJoinFragment( collectionTableAlias, false, true ),
						joinableCollection.whereJoinFragment( collectionTableAlias, false, true )
				);

				// add select fragments from the collection table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				selectStatementBuilder.appendSelectClauseFragment(
						joinableCollection.selectFragment(
								(Joinable) queryableCollection.getElementPersister(),
								ownerTableAlias,
								collectionTableAlias,
								aliasResolutionContext.resolveAliases( fetch ).getEntityElementColumnAliases().getSuffix(),
								aliasResolutionContext.resolveAliases( fetch ).getCollectionColumnAliases().getSuffix(),
								true
						)
				);
			}

			{
				// add join fragments from the collection table -> element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				final String additionalJoinConditions = resolveAdditionalJoinCondition(
						factory,
						elementTableAlias,
						fetchOwner,
						fetch,
						buildingParameters.getQueryInfluencers(),
						aliasResolutionContext
				);

				final String manyToManyFilter = fetch.getCollectionPersister().getManyToManyFilterFragment(
						collectionTableAlias,
						buildingParameters.getQueryInfluencers().getEnabledFilters()
				);

				final String condition;
				if ( "".equals( manyToManyFilter ) ) {
					condition = additionalJoinConditions;
				}
				else if ( "".equals( additionalJoinConditions ) ) {
					condition = manyToManyFilter;
				}
				else {
					condition = additionalJoinConditions + " and " + manyToManyFilter;
				}

				final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();

				addJoins(
						joinFragment,
						elementPersister,
//						JoinType.INNER_JOIN,
						JoinType.LEFT_OUTER_JOIN,
						elementTableAlias,
						elementPersister.getIdentifierColumnNames(),
						StringHelper.qualify( collectionTableAlias, queryableCollection.getElementColumnNames() ),
						condition
				);

				// add select fragments from the element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				final CollectionReferenceAliases aliases = aliasResolutionContext.resolveAliases( fetch );
				selectStatementBuilder.appendSelectClauseFragment(
						elementPersister.selectFragment(
								aliases.getElementTableAlias(),
								aliases.getEntityElementColumnAliases().getSuffix()
						)
				);
			}

			final String manyToManyOrdering = queryableCollection.getManyToManyOrderByString( collectionTableAlias );
			if ( StringHelper.isNotEmpty( manyToManyOrdering ) ) {
				selectStatementBuilder.appendOrderByFragment( manyToManyOrdering );
			}

			final String ordering = queryableCollection.getSQLOrderByString( collectionTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}
		}
		else {
			final String rhsTableAlias = aliasResolutionContext.resolveAliases( fetch ).getElementTableAlias();
			final String[] rhsColumnNames = JoinHelper.getRHSColumnNames( fetch.getFetchedType(), factory );

			final String lhsTableAlias = resolveLhsTableAlias( fetchOwner, fetch, aliasResolutionContext );
			// todo : this is not exactly correct.  it assumes the join refers to the LHS PK
			final String[] aliasedLhsColumnNames = fetch.toSqlSelectFragments( lhsTableAlias );

			final String on = resolveAdditionalJoinCondition(
					factory,
					rhsTableAlias,
					fetchOwner,
					fetch,
					buildingParameters.getQueryInfluencers(),
					aliasResolutionContext
			);

			addJoins(
					joinFragment,
					joinableCollection,
					fetch.isNullable() ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN,
					rhsTableAlias,
					rhsColumnNames,
					aliasedLhsColumnNames,
					on
			);

			// select the "collection columns"
			selectStatementBuilder.appendSelectClauseFragment(
					queryableCollection.selectFragment(
							rhsTableAlias,
							aliasResolutionContext.resolveAliases( fetch ).getCollectionColumnAliases().getSuffix()
					)
			);

			if ( fetch.getCollectionPersister().isOneToMany() ) {
				// if the collection elements are entities, select the entity columns as well
				final CollectionReferenceAliases aliases = aliasResolutionContext.resolveAliases( fetch );
				final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
				selectStatementBuilder.appendSelectClauseFragment(
						elementPersister.selectFragment(
								aliases.getElementTableAlias(),
								aliases.getEntityElementColumnAliases().getSuffix()
						)
				);
			}

			final String ordering = queryableCollection.getSQLOrderByString( rhsTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}
		}

	}

	private static Joinable extractJoinable(FetchOwner fetchOwner) {
		// this is used for collection fetches.  At the end of the day, a fetched collection must be owned by
		// an entity.  Find that entity's persister and return it
		if ( EntityReference.class.isInstance( fetchOwner ) ) {
			return (Joinable) ( (EntityReference) fetchOwner ).getEntityPersister();
		}
		else if ( CompositeFetch.class.isInstance( fetchOwner ) ) {
			return (Joinable) locateCompositeFetchEntityReferenceSource( (CompositeFetch) fetchOwner ).getEntityPersister();
		}
		else if ( EntityElementGraph.class.isInstance( fetchOwner ) ) {
			return (Joinable) ( (EntityElementGraph) fetchOwner ).getEntityPersister();
		}

		throw new IllegalStateException( "Uncertain how to extract Joinable from given FetchOwner : " + fetchOwner );
	}

}
