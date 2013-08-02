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
package org.hibernate.loader.plan2.exec.internal;

import org.jboss.logging.Logger;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan2.exec.process.internal.CollectionReferenceInitializerImpl;
import org.hibernate.loader.plan2.exec.process.internal.EntityReferenceInitializerImpl;
import org.hibernate.loader.plan2.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan2.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan2.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan2.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan2.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;

/**
 * Helper for implementors of entity and collection based query building based on LoadPlans providing common
 * functionality, especially in regards to handling QuerySpace {@link Join}s and {@link Fetch}es.
 * <p/>
 * Exposes 2 main methods:<ol>
 *     <li>{@link #processQuerySpaceJoins(QuerySpace, SelectStatementBuilder)}</li>
 *     <li>{@link #processFetches(FetchSource, SelectStatementBuilder, ReaderCollector)}li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class LoadQueryJoinAndFetchProcessor {
	private static final Logger log = CoreLogging.logger( LoadQueryJoinAndFetchProcessor.class );

	private final AliasResolutionContextImpl aliasResolutionContext;
	private final QueryBuildingParameters buildingParameters;
	private final SessionFactoryImplementor factory;

	/**
	 * Instantiates a LoadQueryBuilderHelper with the given information
	 *
	 * @param aliasResolutionContext
	 * @param buildingParameters
	 * @param factory
	 */
	public LoadQueryJoinAndFetchProcessor(
			AliasResolutionContextImpl aliasResolutionContext,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		this.aliasResolutionContext = aliasResolutionContext;
		this.buildingParameters = buildingParameters;
		this.factory = factory;
	}

	public void processQuerySpaceJoins(QuerySpace querySpace, SelectStatementBuilder selectStatementBuilder) {
		final JoinFragment joinFragment = factory.getDialect().createOuterJoinFragment();
		processQuerySpaceJoins( querySpace, joinFragment );

		selectStatementBuilder.setOuterJoins(
				joinFragment.toFromFragmentString(),
				joinFragment.toWhereFragmentString()
		);
	}

	private void processQuerySpaceJoins(QuerySpace querySpace, JoinFragment joinFragment) {
		// IMPL NOTES:
		//
		// 1) The querySpace and the left-hand-side of each of the querySpace's joins should really be the same.
		// validate that?  any cases where they wont be the same?
		//
		// 2) Assume that the table fragments for the left-hand-side have already been rendered.  We just need to
		// figure out the proper lhs table alias to use and the column/formula from the lhs to define the join
		// condition, which can be different per Join

		for ( Join join : querySpace.getJoins() ) {
			processQuerySpaceJoin( join, joinFragment );
		}
	}

	private void processQuerySpaceJoin(Join join, JoinFragment joinFragment) {
		renderJoin( join, joinFragment );
		processQuerySpaceJoins( join.getRightHandSide(), joinFragment );
	}

	private void renderJoin(Join join, JoinFragment joinFragment) {
		if ( CompositeQuerySpace.class.isInstance( join.getRightHandSide() ) ) {
			handleCompositeJoin( join, joinFragment );
		}
		else if ( EntityQuerySpace.class.isInstance( join.getRightHandSide() ) ) {
			// do not render the entity join for a one-to-many association, since the collection join
			// already joins to the associated entity table (see doc in renderCollectionJoin()).
			if ( join.getLeftHandSide().getDisposition() != QuerySpace.Disposition.COLLECTION ||
					! CollectionQuerySpace.class.cast( join.getLeftHandSide() ).getCollectionPersister().isOneToMany() ) {
				renderEntityJoin( join, joinFragment );
			}
		}
		else if ( CollectionQuerySpace.class.isInstance( join.getRightHandSide() ) ) {
			renderCollectionJoin( join, joinFragment );
		}
	}

	private void handleCompositeJoin(Join join, JoinFragment joinFragment) {
		final String leftHandSideUid = join.getLeftHandSide().getUid();
		final String rightHandSideUid = join.getRightHandSide().getUid();

		final String leftHandSideTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid( leftHandSideUid );
		if ( leftHandSideTableAlias == null ) {
			throw new IllegalStateException(
					"QuerySpace with that UID was not yet registered in the AliasResolutionContext"
			);
		}

		aliasResolutionContext.registerCompositeQuerySpaceUidResolution( rightHandSideUid, leftHandSideTableAlias );
	}

	private void renderEntityJoin(Join join, JoinFragment joinFragment) {
		final String leftHandSideUid = join.getLeftHandSide().getUid();
		final String leftHandSideTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid( leftHandSideUid );
		if ( leftHandSideTableAlias == null ) {
			throw new IllegalStateException( "QuerySpace with that UID was not yet registered in the AliasResolutionContext" );
		}

		final String[] aliasedLhsColumnNames = join.resolveAliasedLeftHandSideJoinConditionColumns( leftHandSideTableAlias );

		final EntityQuerySpace rightHandSide = (EntityQuerySpace) join.getRightHandSide();

		// see if there is already aliases registered for this entity query space (collection joins)
		EntityReferenceAliases aliases = aliasResolutionContext.resolveEntityReferenceAliases( rightHandSide.getUid() );
		if ( aliases == null ) {
				aliases = aliasResolutionContext.generateEntityReferenceAliases(
					rightHandSide.getUid(),
					rightHandSide.getEntityPersister()
			);
		}

		final String[] rhsColumnNames = join.resolveNonAliasedRightHandSideJoinConditionColumns();
		final String rhsTableAlias = aliases.getTableAlias();

		final String additionalJoinConditions = resolveAdditionalJoinCondition(
				rhsTableAlias,
				join.getAnyAdditionalJoinConditions( rhsTableAlias ),
				(Joinable) rightHandSide.getEntityPersister()
		);

		final Joinable joinable = (Joinable) rightHandSide.getEntityPersister();
		addJoins(
				joinFragment,
				joinable,
				join.isRightHandSideRequired() ? JoinType.INNER_JOIN : JoinType.LEFT_OUTER_JOIN,
				aliases.getTableAlias(),
				rhsColumnNames,
				aliasedLhsColumnNames,
				additionalJoinConditions
		);
	}

	private String resolveAdditionalJoinCondition(String rhsTableAlias, String withClause, Joinable joinable) {
		// turns out that the call to AssociationType#getOnCondition in the initial code really just translates to
		// calls to the Joinable.filterFragment() method where the Joinable is either the entity or
		// collection persister
		final String filter = joinable.filterFragment(
				rhsTableAlias,
				buildingParameters.getQueryInfluencers().getEnabledFilters()
		);

		if ( StringHelper.isEmpty( withClause ) && StringHelper.isEmpty( filter ) ) {
			return "";
		}
		else if ( StringHelper.isNotEmpty( withClause ) && StringHelper.isNotEmpty( filter ) ) {
			return filter + " and " + withClause;
		}
		else {
			// only one is non-empty...
			return StringHelper.isNotEmpty( filter ) ? filter : withClause;
		}
	}

	private static void addJoins(
			JoinFragment joinFragment,
			Joinable joinable,
			JoinType joinType,
			String rhsAlias,
			String[] rhsColumnNames,
			String[] aliasedLhsColumnNames,
			String additionalJoinConditions) {
		// somewhere, one of these being empty is causing trouble...
		if ( StringHelper.isEmpty( rhsAlias ) ) {
			throw new IllegalStateException( "Join's RHS table alias cannot be empty" );
		}

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

	private void renderCollectionJoin(Join join, JoinFragment joinFragment) {
		final String leftHandSideUid = join.getLeftHandSide().getUid();
		final String leftHandSideTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid( leftHandSideUid );
		if ( leftHandSideTableAlias == null ) {
			throw new IllegalStateException( "QuerySpace with that UID was not yet registered in the AliasResolutionContext" );
		}
		final String[] aliasedLhsColumnNames = join.resolveAliasedLeftHandSideJoinConditionColumns( leftHandSideTableAlias );

		final CollectionQuerySpace rightHandSide = (CollectionQuerySpace) join.getRightHandSide();
		final CollectionReferenceAliases aliases = aliasResolutionContext.generateCollectionReferenceAliases(
				rightHandSide.getUid(),
				rightHandSide.getCollectionPersister()
		);

		// The SQL join to the "collection table" needs to be rendered.
		//
		// In the case of a basic collection, that's the only join needed.
		//
		// For one-to-many/many-to-many, we need to render the "collection table join"
		// here (as already stated). There will be a follow-on join (rhs will have a join) for the associated entity.
		// For many-to-many, the follow-on join will join to the associated entity element table. For one-to-many,
		// the collection table is the associated entity table, so the follow-on join will not be rendered..

		if ( rightHandSide.getCollectionPersister().isOneToMany()
				|| rightHandSide.getCollectionPersister().isManyToMany() ) {
			// relatedly, for collections with entity elements (one-to-many, many-to-many) we need to register the
			// sql aliases to use for the entity.
			//
			// currently we do not explicitly track the joins under the CollectionQuerySpace to know which is
			// the element join and which is the index join (maybe we should?).  Another option here is to have the
			// "collection join" act as the entity element join in this case (much like I do with entity identifiers).
			// The difficulty there is that collections can theoretically could be multiple joins in that case (one
			// for element, one for index).  However, that's a bit of future-planning as today Hibernate does not
			// properly deal with the index anyway in terms of allowing dynamic fetching across a collection index...
			//
			// long story short, for now we'll use an assumption that the last join in the CollectionQuerySpace is the
			// element join (that's how the joins are built as of now..)
			//
			// todo : remove this assumption ^^; maybe we make CollectionQuerySpace "special" and rather than have it
			// hold a list of joins, we have it expose the 2 (index, element) separately.

			Join collectionElementJoin = null;
			for ( Join collectionJoin : rightHandSide.getJoins() ) {
				collectionElementJoin = collectionJoin;
			}
			if ( collectionElementJoin == null ) {
				throw new IllegalStateException(
						String.format(
								"Could not locate collection element join within collection join [%s : %s]",
								rightHandSide.getUid(),
								rightHandSide.getCollectionPersister()
						)
				);
			}
			aliasResolutionContext.registerQuerySpaceAliases(
					collectionElementJoin.getRightHandSide().getUid(),
					new EntityReferenceAliasesImpl(
							aliases.getElementTableAlias(),
							aliases.getEntityElementColumnAliases()
					)
			);
		}

		renderSqlJoinToCollectionTable(
				aliases,
				rightHandSide,
				aliasedLhsColumnNames,
				join,
				joinFragment
		);

//		if ( rightHandSide.getCollectionPersister().isManyToMany() ) {
//			renderManyToManyJoin(
//					aliases,
//					rightHandSide,
//					aliasedLhsColumnNames,
//					join,
//					joinFragment
//			);
//		}
//		else if ( rightHandSide.getCollectionPersister().isOneToMany() ) {
//			renderOneToManyJoin(
//					aliases,
//					rightHandSide,
//					aliasedLhsColumnNames,
//					join,
//					joinFragment
//			);
//		}
//		else {
//			renderBasicCollectionJoin(
//					aliases,
//					rightHandSide,
//					aliasedLhsColumnNames,
//					join,
//					joinFragment
//			);
//		}
	}

	private void renderSqlJoinToCollectionTable(
			CollectionReferenceAliases aliases,
			CollectionQuerySpace rightHandSide,
			String[] aliasedLhsColumnNames,
			Join join,
			JoinFragment joinFragment) {
		final String collectionTableAlias = aliases.getCollectionTableAlias();

		final CollectionPersister persister = rightHandSide.getCollectionPersister();
		final QueryableCollection queryableCollection = (QueryableCollection) persister;

		// add join fragments from the owner table -> collection table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final String filterFragment = queryableCollection.filterFragment(
				collectionTableAlias,
				buildingParameters.getQueryInfluencers().getEnabledFilters()
		);

		joinFragment.addJoin(
				queryableCollection.getTableName(),
				collectionTableAlias,
				aliasedLhsColumnNames,
				queryableCollection.getKeyColumnNames(),
				JoinType.LEFT_OUTER_JOIN,
				filterFragment
		);
		joinFragment.addJoins(
				queryableCollection.fromJoinFragment( collectionTableAlias, false, true ),
				queryableCollection.whereJoinFragment( collectionTableAlias, false, true )
		);
	}

//	private void renderManyToManyJoin(
//			CollectionReferenceAliases aliases,
//			CollectionQuerySpace rightHandSide,
//			String[] aliasedLhsColumnNames,
//			Join join,
//			JoinFragment joinFragment) {
//		final CollectionPersister persister = rightHandSide.getCollectionPersister();
//		final QueryableCollection queryableCollection = (QueryableCollection) persister;
//
//		// for many-to-many we have 3 table aliases.  By way of example, consider a normal m-n: User<->Role
//		// where User is the FetchOwner and Role (User.roles) is the Fetch.  We'd have:
//		//		1) the owner's table : user - in terms of rendering the joins (not the fetch select fragments), the
//		// 			lhs table alias is only needed to qualify the lhs join columns, but we already have the qualified
//		// 			columns here (aliasedLhsColumnNames)
//		//final String ownerTableAlias = ...;
//		//		2) the m-n table : user_role
//		final String collectionTableAlias = aliases.getCollectionTableAlias();
//		//		3) the element table : role
//		final String elementTableAlias = aliases.getElementTableAlias();
//
//		// somewhere, one of these being empty is causing trouble...
//		if ( StringHelper.isEmpty( collectionTableAlias ) ) {
//			throw new IllegalStateException( "Collection table alias cannot be empty" );
//		}
//		if ( StringHelper.isEmpty( elementTableAlias ) ) {
//			throw new IllegalStateException( "Collection element (many-to-many) table alias cannot be empty" );
//		}
//
//
//		{
//			// add join fragments from the owner table -> collection table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			final String filterFragment = queryableCollection.filterFragment(
//					collectionTableAlias,
//					buildingParameters.getQueryInfluencers().getEnabledFilters()
//			);
//
//			joinFragment.addJoin(
//					queryableCollection.getTableName(),
//					collectionTableAlias,
//					aliasedLhsColumnNames,
//					queryableCollection.getKeyColumnNames(),
//					JoinType.LEFT_OUTER_JOIN,
//					filterFragment
//			);
//			joinFragment.addJoins(
//					queryableCollection.fromJoinFragment( collectionTableAlias, false, true ),
//					queryableCollection.whereJoinFragment( collectionTableAlias, false, true )
//			);
//		}
//
//		{
//			// add join fragments from the collection table -> element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			final String additionalJoinConditions = resolveAdditionalJoinCondition(
//					elementTableAlias,
//					join.getAnyAdditionalJoinConditions( elementTableAlias ),
//					queryableCollection
//			);
//
//			final String manyToManyFilter = persister.getManyToManyFilterFragment(
//					collectionTableAlias,
//					buildingParameters.getQueryInfluencers().getEnabledFilters()
//			);
//
//			final String condition;
//			if ( StringHelper.isEmpty( manyToManyFilter ) ) {
//				condition = additionalJoinConditions;
//			}
//			else if ( StringHelper.isEmpty( additionalJoinConditions ) ) {
//				condition = manyToManyFilter;
//			}
//			else {
//				condition = additionalJoinConditions + " and " + manyToManyFilter;
//			}
//
//			final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
//
//			addJoins(
//					joinFragment,
//					elementPersister,
//					JoinType.LEFT_OUTER_JOIN,
//					elementTableAlias,
//					elementPersister.getIdentifierColumnNames(),
//					StringHelper.qualify( collectionTableAlias, queryableCollection.getElementColumnNames() ),
//					condition
//			);
//		}
//	}
//
//	private void renderOneToManyJoin(
//			CollectionReferenceAliases aliases,
//			CollectionQuerySpace rightHandSide,
//			String[] aliasedLhsColumnNames,
//			Join join,
//			JoinFragment joinFragment) {
//		final QueryableCollection queryableCollection = (QueryableCollection) rightHandSide.getCollectionPersister();
//
//		final String rhsTableAlias = aliases.getElementTableAlias();
//		final String[] rhsColumnNames = join.resolveNonAliasedRightHandSideJoinConditionColumns();
//
//		final String on = resolveAdditionalJoinCondition(
//				rhsTableAlias,
//				join.getAnyAdditionalJoinConditions( rhsTableAlias ),
//				queryableCollection
//		);
//
//		addJoins(
//				joinFragment,
//				queryableCollection,
//				JoinType.LEFT_OUTER_JOIN,
//				rhsTableAlias,
//				rhsColumnNames,
//				aliasedLhsColumnNames,
//				on
//		);
//	}
//
//	private void renderBasicCollectionJoin(
//			CollectionReferenceAliases aliases,
//			CollectionQuerySpace rightHandSide,
//			String[] aliasedLhsColumnNames,
//			Join join,
//			JoinFragment joinFragment) {
//		final QueryableCollection queryableCollection = (QueryableCollection) rightHandSide.getCollectionPersister();
//
//		final String rhsTableAlias = aliases.getElementTableAlias();
//		final String[] rhsColumnNames = join.resolveNonAliasedRightHandSideJoinConditionColumns();
//
//		final String on = resolveAdditionalJoinCondition(
//				rhsTableAlias,
//				join.getAnyAdditionalJoinConditions( rhsTableAlias ),
//				queryableCollection
//		);
//
//		addJoins(
//				joinFragment,
//				queryableCollection,
//				JoinType.LEFT_OUTER_JOIN,
//				rhsTableAlias,
//				rhsColumnNames,
//				aliasedLhsColumnNames,
//				on
//		);
//	}







	public FetchStats processFetches(
			FetchSource fetchSource,
			SelectStatementBuilder selectStatementBuilder,
			ReaderCollector readerCollector) {
		final FetchStatsImpl fetchStats = new FetchStatsImpl();

		// if the fetchSource is an entityReference, we should also walk its identifier fetches here...
		//
		// what if fetchSource is a composite fetch (as it would be in the case of a key-many-to-one)?
		if ( EntityReference.class.isInstance( fetchSource ) ) {
			final EntityReference fetchOwnerAsEntityReference = (EntityReference) fetchSource;
			if ( fetchOwnerAsEntityReference.getIdentifierDescription().hasFetches() ) {
				final FetchSource entityIdentifierAsFetchSource = (FetchSource) fetchOwnerAsEntityReference.getIdentifierDescription();
				for ( Fetch fetch : entityIdentifierAsFetchSource.getFetches() ) {
					processFetch(
							selectStatementBuilder,
							fetchSource,
							fetch,
							readerCollector,
							fetchStats
					);
				}
			}
		}

		processFetches( fetchSource, selectStatementBuilder, readerCollector, fetchStats );
		return fetchStats;
	}

	private void processFetches(
			FetchSource fetchSource,
			SelectStatementBuilder selectStatementBuilder,
			ReaderCollector readerCollector,
			FetchStatsImpl fetchStats) {
		for ( Fetch fetch : fetchSource.getFetches() ) {
			processFetch(
					selectStatementBuilder,
					fetchSource,
					fetch,
					readerCollector,
					fetchStats
			);
		}
	}


	private void processFetch(
			SelectStatementBuilder selectStatementBuilder,
			FetchSource fetchSource,
			Fetch fetch,
			ReaderCollector readerCollector,
			FetchStatsImpl fetchStats) {
		if ( ! FetchStrategyHelper.isJoinFetched( fetch.getFetchStrategy() ) ) {
			return;
		}

		if ( EntityFetch.class.isInstance( fetch ) ) {
			final EntityFetch entityFetch = (EntityFetch) fetch;
			processEntityFetch(
					selectStatementBuilder,
					fetchSource,
					entityFetch,
					readerCollector,
					fetchStats
			);
		}
		else if ( CollectionFetch.class.isInstance( fetch ) ) {
			final CollectionFetch collectionFetch = (CollectionFetch) fetch;
			processCollectionFetch(
					selectStatementBuilder,
					fetchSource,
					collectionFetch,
					readerCollector,
					fetchStats
			);
		}
		else {
			// could also be a CompositeFetch, we ignore those here
			// but do still need to visit their fetches...
			if ( FetchSource.class.isInstance( fetch ) ) {
				processFetches(
						(FetchSource) fetch,
						selectStatementBuilder,
						readerCollector,
						fetchStats
				);
			}
		}
	}

	private void processEntityFetch(
			SelectStatementBuilder selectStatementBuilder,
			FetchSource fetchSource,
			EntityFetch fetch,
			ReaderCollector readerCollector,
			FetchStatsImpl fetchStats) {
		// todo : still need to think through expressing bi-directionality in the new model...
//		if ( BidirectionalEntityFetch.class.isInstance( fetch ) ) {
//			log.tracef( "Skipping bi-directional entity fetch [%s]", fetch );
//			return;
//		}

		fetchStats.processingFetch( fetch );

		// First write out the SQL SELECT fragments
		final Joinable joinable = (Joinable) fetch.getEntityPersister();
		final EntityReferenceAliases aliases = aliasResolutionContext.resolveEntityReferenceAliases(
				fetch.getQuerySpaceUid()
		);
		// the null arguments here relate to many-to-many fetches
		selectStatementBuilder.appendSelectClauseFragment(
				joinable.selectFragment(
						null,
						null,
						aliases.getTableAlias(),
						aliases.getColumnAliases().getSuffix(),
						null,
						true
				)
		);

//		// process its identifier fetches first (building EntityReferenceInitializers for them if needed)
//		if ( EntityReference.class.isInstance( fetchSource ) ) {
//			final EntityReference fetchOwnerAsEntityReference = (EntityReference) fetchSource;
//			if ( fetchOwnerAsEntityReference.getIdentifierDescription().hasFetches() ) {
//				final FetchSource entityIdentifierAsFetchSource = (FetchSource) fetchOwnerAsEntityReference.getIdentifierDescription();
//				for ( Fetch identifierFetch : entityIdentifierAsFetchSource.getFetches() ) {
//					processFetch(
//							selectStatementBuilder,
//							fetchSource,
//							identifierFetch,
//							readerCollector,
//							fetchStats
//					);
//				}
//			}
//		}

		// build an EntityReferenceInitializers for the incoming fetch itself
		readerCollector.add( new EntityReferenceInitializerImpl( fetch, aliases ) );

		// then visit each of our (non-identifier) fetches
		processFetches( fetch, selectStatementBuilder, readerCollector, fetchStats );
	}

	private void processCollectionFetch(
			SelectStatementBuilder selectStatementBuilder,
			FetchSource fetchSource,
			CollectionFetch fetch,
			ReaderCollector readerCollector,
			FetchStatsImpl fetchStats) {
		fetchStats.processingFetch( fetch );

		final CollectionReferenceAliases aliases = aliasResolutionContext.resolveCollectionReferenceAliases( fetch.getQuerySpaceUid() );

		final QueryableCollection queryableCollection = (QueryableCollection) fetch.getCollectionPersister();
		final Joinable joinableCollection = (Joinable) fetch.getCollectionPersister();

		if ( fetch.getCollectionPersister().isManyToMany() ) {
			// todo : better way to access `ownerTableAlias` here.
			// 		when processing the Join part of this we are able to look up the "lhs table alias" because we know
			// 		the 'lhs' QuerySpace.
			//
			// Good idea to be able resolve a Join by lookup on the rhs and lhs uid?  If so, Fetch

			// for many-to-many we have 3 table aliases.  By way of example, consider a normal m-n: User<->Role
			// where User is the FetchOwner and Role (User.roles) is the Fetch.  We'd have:
			//		1) the owner's table : user
			final String ownerTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid( fetchSource.getQuerySpaceUid() );
			//		2) the m-n table : user_role
			final String collectionTableAlias = aliases.getCollectionTableAlias();
			//		3) the element table : role
			final String elementTableAlias = aliases.getElementTableAlias();

			// add select fragments from the collection table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			selectStatementBuilder.appendSelectClauseFragment(
					joinableCollection.selectFragment(
							(Joinable) queryableCollection.getElementPersister(),
							ownerTableAlias,
							collectionTableAlias,
							aliases.getEntityElementColumnAliases().getSuffix(),
							aliases.getCollectionColumnAliases().getSuffix(),
							true
					)
			);

			// add select fragments from the element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
			selectStatementBuilder.appendSelectClauseFragment(
					elementPersister.selectFragment(
							elementTableAlias,
							aliases.getEntityElementColumnAliases().getSuffix()
					)
			);

			// add SQL ORDER-BY fragments
			final String manyToManyOrdering = queryableCollection.getManyToManyOrderByString( collectionTableAlias );
			if ( StringHelper.isNotEmpty( manyToManyOrdering ) ) {
				selectStatementBuilder.appendOrderByFragment( manyToManyOrdering );
			}

			final String ordering = queryableCollection.getSQLOrderByString( collectionTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}

			// add an EntityReferenceInitializer for the collection elements (keys also?)
			final EntityReferenceAliases entityReferenceAliases = new EntityReferenceAliases() {
				@Override
				public String getTableAlias() {
					return aliases.getElementTableAlias();
				}

				@Override
				public EntityAliases getColumnAliases() {
					return aliases.getEntityElementColumnAliases();
				}
			};
			aliasResolutionContext.registerQuerySpaceAliases( fetch.getQuerySpaceUid(), entityReferenceAliases );
			readerCollector.add(
					new EntityReferenceInitializerImpl(
							(EntityReference) fetch.getElementGraph(),
							entityReferenceAliases
					)
			);
		}
		else {
			final String rhsTableAlias = aliases.getElementTableAlias();

			// select the "collection columns"
			selectStatementBuilder.appendSelectClauseFragment(
					queryableCollection.selectFragment(
							rhsTableAlias,
							aliases.getCollectionColumnAliases().getSuffix()
					)
			);

			if ( fetch.getCollectionPersister().isOneToMany() ) {
				// if the collection elements are entities, select the entity columns as well
				final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
				selectStatementBuilder.appendSelectClauseFragment(
						elementPersister.selectFragment(
								aliases.getElementTableAlias(),
								aliases.getEntityElementColumnAliases().getSuffix()
						)
				);
				final EntityReferenceAliases entityReferenceAliases = new EntityReferenceAliases() {
					@Override
					public String getTableAlias() {
						return aliases.getElementTableAlias();
					}

					@Override
					public EntityAliases getColumnAliases() {
						return aliases.getEntityElementColumnAliases();
					}
				};
				aliasResolutionContext.registerQuerySpaceAliases( fetch.getQuerySpaceUid(), entityReferenceAliases );
				readerCollector.add(
						new EntityReferenceInitializerImpl(
								(EntityReference) fetch.getElementGraph(),
								entityReferenceAliases
						)
				);
			}

			final String ordering = queryableCollection.getSQLOrderByString( rhsTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}
		}

		if ( fetch.getElementGraph() != null ) {
			processFetches( fetch.getElementGraph(), selectStatementBuilder, readerCollector );
		}

		readerCollector.add( new CollectionReferenceInitializerImpl( fetch, aliases ) );
	}

	/**
	 * Implementation of FetchStats
	 */
	private static class FetchStatsImpl implements FetchStats {
		private boolean hasSubselectFetch;

		public void processingFetch(Fetch fetch) {
			if ( ! hasSubselectFetch ) {
				if ( fetch.getFetchStrategy().getStyle() == FetchStyle.SUBSELECT
						&& fetch.getFetchStrategy().getTiming() != FetchTiming.IMMEDIATE ) {
					hasSubselectFetch = true;
				}
			}
		}

		@Override
		public boolean hasSubselectFetches() {
			return hasSubselectFetch;
		}
	}

}
