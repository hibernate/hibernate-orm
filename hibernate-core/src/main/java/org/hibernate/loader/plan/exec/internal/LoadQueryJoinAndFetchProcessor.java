/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceInitializerImpl;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceInitializerImpl;
import org.hibernate.loader.plan.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CollectionQuerySpace;
import org.hibernate.loader.plan.spi.CompositeQuerySpace;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityQuerySpace;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Helper for implementors of entity and collection based query building based on LoadPlans providing common
 * functionality, especially in regards to handling QuerySpace {@link Join}s and {@link Fetch}es.
 * <p/>
 * Exposes 2 main methods:<ol>
 *     <li>{@link #processQuerySpaceJoins(QuerySpace, SelectStatementBuilder)}</li>
 *     <li>{@link #processFetches(FetchSource, SelectStatementBuilder, org.hibernate.loader.plan.exec.process.spi.ReaderCollector)}li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class LoadQueryJoinAndFetchProcessor {
	private static final Logger LOG = CoreLogging.logger( LoadQueryJoinAndFetchProcessor.class );

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

	public AliasResolutionContext getAliasResolutionContext() {
		return aliasResolutionContext;
	}

	public QueryBuildingParameters getQueryBuildingParameters() {
		return buildingParameters;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	public void processQuerySpaceJoins(QuerySpace querySpace, SelectStatementBuilder selectStatementBuilder) {
		LOG.debug( "processing queryspace " + querySpace.getUid() );
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
			if ( join.getLeftHandSide().getDisposition() == QuerySpace.Disposition.COLLECTION ) {
				if ( CollectionQuerySpace.class.cast( join.getLeftHandSide() ).getCollectionPersister().isManyToMany() ) {
					renderManyToManyJoin( join, joinFragment );
				}
				else if ( JoinDefinedByMetadata.class.isInstance( join ) &&
						CollectionPropertyNames.COLLECTION_INDICES.equals( JoinDefinedByMetadata.class.cast( join ).getJoinedPropertyName() ) ) {
					renderManyToManyJoin( join, joinFragment );
				}
			}
			else {
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
		final EntityQuerySpace rightHandSide = (EntityQuerySpace) join.getRightHandSide();

		// see if there is already aliases registered for this entity query space (collection joins)
		EntityReferenceAliases aliases = aliasResolutionContext.resolveEntityReferenceAliases( rightHandSide.getUid() );
		if ( aliases == null ) {
			aliasResolutionContext.generateEntityReferenceAliases(
					rightHandSide.getUid(),
					rightHandSide.getEntityPersister()
			);
		}

		final Joinable joinable = (Joinable) rightHandSide.getEntityPersister();
		addJoins(
				join,
				joinFragment,
				joinable
		);
	}

	private AssociationType getJoinedAssociationTypeOrNull(Join join) {

		if ( !JoinDefinedByMetadata.class.isInstance( join ) ) {
			return null;
		}
		final Type joinedType = ( (JoinDefinedByMetadata) join ).getJoinedPropertyType();
		return joinedType.isAssociationType()
				? (AssociationType) joinedType
				: null;
	}

	private String resolveAdditionalJoinCondition(String rhsTableAlias, String withClause, Joinable joinable, AssociationType associationType) {
		// turns out that the call to AssociationType#getOnCondition in the initial code really just translates to
		// calls to the Joinable.filterFragment() method where the Joinable is either the entity or
		// collection persister
		final String filter = associationType!=null?
				associationType.getOnCondition( rhsTableAlias, factory, buildingParameters.getQueryInfluencers().getEnabledFilters() ):
				joinable.filterFragment(
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

	private void addJoins(
			Join join,
			JoinFragment joinFragment,
			Joinable joinable) {

		final String rhsTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid(
				join.getRightHandSide().getUid()
		);
		if ( StringHelper.isEmpty( rhsTableAlias ) ) {
			throw new IllegalStateException( "Join's RHS table alias cannot be empty" );
		}

		final String lhsTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid(
				join.getLeftHandSide().getUid()
		);
		if ( lhsTableAlias == null ) {
			throw new IllegalStateException( "QuerySpace with that UID was not yet registered in the AliasResolutionContext" );
		}

		// add join fragments from the collection table -> element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final String additionalJoinConditions = resolveAdditionalJoinCondition(
				rhsTableAlias,
				join.getAnyAdditionalJoinConditions( rhsTableAlias ),
				joinable,
				getJoinedAssociationTypeOrNull( join )
		);

		joinFragment.addJoin(
				joinable.getTableName(),
				rhsTableAlias,
				join.resolveAliasedLeftHandSideJoinConditionColumns( lhsTableAlias ),
				join.resolveNonAliasedRightHandSideJoinConditionColumns(),
				join.isRightHandSideRequired() ? JoinType.INNER_JOIN : JoinType.LEFT_OUTER_JOIN,
				additionalJoinConditions
		);
		joinFragment.addJoins(
				joinable.fromJoinFragment( rhsTableAlias, false, true ),
				joinable.whereJoinFragment( rhsTableAlias, false, true )
		);
	}

	private void renderCollectionJoin(Join join, JoinFragment joinFragment) {
		final CollectionQuerySpace rightHandSide = (CollectionQuerySpace) join.getRightHandSide();

		// The SQL join to the "collection table" needs to be rendered.
		//
		// In the case of a basic collection, that's the only join needed.
		//
		// For one-to-many/many-to-many, we need to render the "collection table join"
		// here (as already stated). There will be a follow-on join (rhs will have a join) for the associated entity.
		// For many-to-many, the follow-on join will join to the associated entity element table. For one-to-many,
		// the collection table is the associated entity table, so the follow-on join will not be rendered..

		// currently we do not explicitly track the joins under the CollectionQuerySpace to know which is
		// the element join and which is the index join (maybe we should?).

		JoinDefinedByMetadata collectionElementJoin = null;
		JoinDefinedByMetadata collectionIndexJoin = null;
		for ( Join collectionJoin : rightHandSide.getJoins() ) {
			if ( JoinDefinedByMetadata.class.isInstance( collectionJoin ) ) {
				final JoinDefinedByMetadata collectionJoinDefinedByMetadata = (JoinDefinedByMetadata) collectionJoin;
				if ( CollectionPropertyNames.COLLECTION_ELEMENTS.equals( collectionJoinDefinedByMetadata.getJoinedPropertyName() ) ) {
					if ( collectionElementJoin != null ) {
						throw new AssertionFailure(
								String.format(
										"More than one element join defined for: %s",
										rightHandSide.getCollectionPersister().getRole()
								)
						);
					}
					collectionElementJoin = collectionJoinDefinedByMetadata;
				}
				if ( CollectionPropertyNames.COLLECTION_INDICES.equals( collectionJoinDefinedByMetadata.getJoinedPropertyName() ) ) {
					if ( collectionIndexJoin != null ) {
						throw new AssertionFailure(
								String.format(
										"More than one index join defined for: %s",
										rightHandSide.getCollectionPersister().getRole()
								)
						);
					}
					collectionIndexJoin = collectionJoinDefinedByMetadata;
				}
			}
		}

		if ( rightHandSide.getCollectionPersister().isOneToMany()
				|| rightHandSide.getCollectionPersister().isManyToMany() ) {
			// relatedly, for collections with entity elements (one-to-many, many-to-many) we need to register the
			// sql aliases to use for the entity.
			if ( collectionElementJoin == null ) {
				throw new IllegalStateException(
						String.format(
								"Could not locate collection element join within collection join [%s : %s]",
								rightHandSide.getUid(),
								rightHandSide.getCollectionPersister()
						)
				);
			}
			aliasResolutionContext.generateCollectionReferenceAliases(
					rightHandSide.getUid(),
					rightHandSide.getCollectionPersister(),
					collectionElementJoin.getRightHandSide().getUid()
			);
		}
		else {
			aliasResolutionContext.generateCollectionReferenceAliases(
					rightHandSide.getUid(),
					rightHandSide.getCollectionPersister(),
					null
			);
		}

		if ( rightHandSide.getCollectionPersister().hasIndex() &&
				rightHandSide.getCollectionPersister().getIndexType().isEntityType() ) {
			// for collections with entity index we need to register the
			// sql aliases to use for the entity.
			if ( collectionIndexJoin == null ) {
				throw new IllegalStateException(
						String.format(
								"Could not locate collection index join within collection join [%s : %s]",
								rightHandSide.getUid(),
								rightHandSide.getCollectionPersister()
						)
				);
			}
			aliasResolutionContext.generateEntityReferenceAliases(
					collectionIndexJoin.getRightHandSide().getUid(),
					rightHandSide.getCollectionPersister().getIndexDefinition().toEntityDefinition().getEntityPersister()
			);
		}
		addJoins(
				join,
				joinFragment,
				(Joinable) rightHandSide.getCollectionPersister()
		);
	}

	private void renderManyToManyJoin(
			Join join,
			JoinFragment joinFragment) {

		// for many-to-many we have 3 table aliases.  By way of example, consider a normal m-n: User<->Role
		// where User is the FetchOwner and Role (User.roles) is the Fetch.  We'd have:
		//		1) the owner's table : user - in terms of rendering the joins (not the fetch select fragments), the
		// 			lhs table alias is only needed to qualify the lhs join columns, but we already have the qualified
		// 			columns here (aliasedLhsColumnNames)
		//final String ownerTableAlias = ...;
		//		2) the m-n table : user_role
		//		3) the element table : role
		final EntityPersister entityPersister = ( (EntityQuerySpace) join.getRightHandSide() ).getEntityPersister();
		final String entityTableAlias = aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid(
			join.getRightHandSide().getUid()
		);

		if ( StringHelper.isEmpty( entityTableAlias ) ) {
			throw new IllegalStateException( "Collection element (many-to-many) table alias cannot be empty" );
		}
		if ( JoinDefinedByMetadata.class.isInstance( join ) &&
				CollectionPropertyNames.COLLECTION_ELEMENTS.equals( ( (JoinDefinedByMetadata) join ).getJoinedPropertyName() ) ) {
			final CollectionQuerySpace leftHandSide = (CollectionQuerySpace) join.getLeftHandSide();
			final CollectionPersister persister = leftHandSide.getCollectionPersister();
			final String manyToManyFilter = persister.getManyToManyFilterFragment(
					entityTableAlias,
					buildingParameters.getQueryInfluencers().getEnabledFilters()
			);
			joinFragment.addCondition( manyToManyFilter );
		}

		addJoins(
				join,
				joinFragment,
				(Joinable) entityPersister
		);
	}

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
		else if ( CollectionAttributeFetch.class.isInstance( fetch ) ) {
			final CollectionAttributeFetch collectionFetch = (CollectionAttributeFetch) fetch;
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
		EntityReferenceAliases aliases = aliasResolutionContext.resolveEntityReferenceAliases(
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

		// process its identifier fetches first (building EntityReferenceInitializers for them if needed)
		if ( fetch.getIdentifierDescription().hasFetches() ) {
			final FetchSource entityIdentifierAsFetchSource = (FetchSource) fetch.getIdentifierDescription();
			for ( Fetch identifierFetch : entityIdentifierAsFetchSource.getFetches() ) {
				processFetch(
						selectStatementBuilder,
						fetch,
						identifierFetch,
						readerCollector,
						fetchStats
				);
			}
		}

		// build an EntityReferenceInitializers for the incoming fetch itself
		readerCollector.add( new EntityReferenceInitializerImpl( fetch, aliases ) );

		// then visit each of our (non-identifier) fetches
		processFetches( fetch, selectStatementBuilder, readerCollector, fetchStats );
	}

	private void processCollectionFetch(
			SelectStatementBuilder selectStatementBuilder,
			FetchSource fetchSource,
			CollectionAttributeFetch fetch,
			ReaderCollector readerCollector,
			FetchStatsImpl fetchStats) {
		fetchStats.processingFetch( fetch );

		final CollectionReferenceAliases aliases = aliasResolutionContext.resolveCollectionReferenceAliases(
				fetch.getQuerySpaceUid()
		);

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
							elementTableAlias,
							collectionTableAlias,

							aliases.getEntityElementAliases().getColumnAliases().getSuffix(),
							aliases.getCollectionColumnAliases().getSuffix(),
							true
					)
			);

			// add select fragments from the element entity table ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
			selectStatementBuilder.appendSelectClauseFragment(
					elementPersister.selectFragment(
							elementTableAlias,
							aliases.getEntityElementAliases().getColumnAliases().getSuffix()
					)
			);

			// add SQL ORDER-BY fragments
			final String manyToManyOrdering = queryableCollection.getManyToManyOrderByString( elementTableAlias );
			if ( StringHelper.isNotEmpty( manyToManyOrdering ) ) {
				selectStatementBuilder.appendOrderByFragment( manyToManyOrdering );
			}

			final String ordering = queryableCollection.getSQLOrderByString( collectionTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}

			readerCollector.add(
					new EntityReferenceInitializerImpl(
							(EntityReference) fetch.getElementGraph(),
							aliasResolutionContext.resolveEntityReferenceAliases( fetch.getElementGraph().getQuerySpaceUid() )
					)
			);
		}
		else {
			// select the "collection columns"
			selectStatementBuilder.appendSelectClauseFragment(
					queryableCollection.selectFragment(
							aliases.getElementTableAlias(),
							aliases.getCollectionColumnAliases().getSuffix()
					)
			);

			if ( fetch.getCollectionPersister().isOneToMany() ) {
				// if the collection elements are entities, select the entity columns as well
				final OuterJoinLoadable elementPersister = (OuterJoinLoadable) queryableCollection.getElementPersister();
				selectStatementBuilder.appendSelectClauseFragment(
						elementPersister.selectFragment(
								aliases.getElementTableAlias(),
								aliases.getEntityElementAliases().getColumnAliases().getSuffix()
						)
				);
				readerCollector.add(
						new EntityReferenceInitializerImpl(
								(EntityReference) fetch.getElementGraph(),
								aliasResolutionContext.resolveEntityReferenceAliases( fetch.getElementGraph().getQuerySpaceUid() )
						)
				);
			}

			final String ordering = queryableCollection.getSQLOrderByString( aliases.getElementTableAlias() );
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
