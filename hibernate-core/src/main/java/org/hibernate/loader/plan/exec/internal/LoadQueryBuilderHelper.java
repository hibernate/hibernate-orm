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
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.EntityIdentifierReader;
import org.hibernate.loader.plan.exec.process.internal.EntityIdentifierReaderImpl;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceReader;
import org.hibernate.loader.plan.exec.process.internal.OneToOneFetchReader;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.exec.spi.ReaderCollector;
import org.hibernate.loader.plan.spi.AnyFetch;
import org.hibernate.loader.plan.spi.BidirectionalEntityFetch;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeElementGraph;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.CompositeIndexGraph;
import org.hibernate.loader.plan.spi.EntityElementGraph;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * Helper for implementors of entity and collection based query building based on LoadPlans providing common
 * functionality
 *
 * @author Steve Ebersole
 */
public class LoadQueryBuilderHelper {
	private static final Logger log = CoreLogging.logger( LoadQueryBuilderHelper.class );

	private LoadQueryBuilderHelper() {
	}

	// used to collect information about fetches.  For now that is only whether there were subselect fetches
	public static interface FetchStats {
		public boolean hasSubselectFetches();
	}

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

	public static void applyIdentifierJoinFetches(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			FetchOwner fetchOwner,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector) {

	}

	public static FetchStats applyJoinFetches(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			FetchOwner fetchOwner,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector) {

		final JoinFragment joinFragment = factory.getDialect().createOuterJoinFragment();
		final FetchStatsImpl stats = new FetchStatsImpl();

		// if the fetch owner is an entityReference, we should also walk its identifier fetches here...
		//
		// what if fetchOwner is a composite fetch (as it would be in the case of a key-many-to-one)?
		if ( EntityReference.class.isInstance( fetchOwner ) ) {
			final EntityReference fetchOwnerAsEntityReference = (EntityReference) fetchOwner;
			for ( Fetch fetch : fetchOwnerAsEntityReference.getIdentifierDescription().getFetches() ) {
				processFetch(
						selectStatementBuilder,
						factory,
						joinFragment,
						fetchOwner,
						fetch,
						buildingParameters,
						aliasResolutionContext,
						readerCollector,
						stats
				);
			}
		}

		processJoinFetches(
				selectStatementBuilder,
				factory,
				joinFragment,
				fetchOwner,
				buildingParameters,
				aliasResolutionContext,
				readerCollector,
				stats
		);

		selectStatementBuilder.setOuterJoins(
				joinFragment.toFromFragmentString(),
				joinFragment.toWhereFragmentString()
		);

		return stats;
	}


	private static void processJoinFetches(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			FetchStatsImpl stats) {

		for ( Fetch fetch : fetchOwner.getFetches() ) {
			processFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					fetch,
					buildingParameters,
					aliasResolutionContext,
					readerCollector,
					stats
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
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			FetchStatsImpl stats) {
		if ( ! FetchStrategyHelper.isJoinFetched( fetch.getFetchStrategy() ) ) {
			return;
		}

		if ( EntityFetch.class.isInstance( fetch ) ) {
			final EntityFetch entityFetch = (EntityFetch) fetch;
			processEntityFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					entityFetch,
					buildingParameters,
					aliasResolutionContext,
					readerCollector,
					stats
			);
		}
		else if ( CollectionFetch.class.isInstance( fetch ) ) {
			final CollectionFetch collectionFetch = (CollectionFetch) fetch;
			processCollectionFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					fetchOwner,
					collectionFetch,
					buildingParameters,
					aliasResolutionContext,
					readerCollector,
					stats
			);
			if ( collectionFetch.getIndexGraph() != null ) {
				processJoinFetches(
						selectStatementBuilder,
						factory,
						joinFragment,
						collectionFetch.getIndexGraph(),
						buildingParameters,
						aliasResolutionContext,
						readerCollector,
						stats
				);
			}
			if ( collectionFetch.getElementGraph() != null ) {
				processJoinFetches(
						selectStatementBuilder,
						factory,
						joinFragment,
						collectionFetch.getElementGraph(),
						buildingParameters,
						aliasResolutionContext,
						readerCollector,
						stats
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
						aliasResolutionContext,
						readerCollector,
						stats
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
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			FetchStatsImpl stats) {
		if ( BidirectionalEntityFetch.class.isInstance( fetch ) ) {
			log.tracef( "Skipping bi-directional entity fetch [%s]", fetch );
			return;
		}

		stats.processingFetch( fetch );

		// write the fragments for this fetch to the in-flight SQL builder
		final EntityReferenceAliases aliases = renderSqlFragments(
				selectStatementBuilder,
				factory,
				joinFragment,
				fetchOwner,
				fetch,
				buildingParameters,
				aliasResolutionContext
		);

		// now we build readers as follows:
		//		1) readers for any fetches that are part of the identifier
		final EntityIdentifierReader identifierReader = buildIdentifierReader(
				selectStatementBuilder,
				factory,
				joinFragment,
				fetch,
				buildingParameters,
				aliasResolutionContext,
				readerCollector,
				aliases,
				stats
		);

		//		2) a reader for this fetch itself
		// 			todo : not sure this distinction really matters aside form the whole "register nullable property" stuff,
		// 			but not sure we need a distinct class for just that
		if ( fetch.getFetchedType().isOneToOne() ) {
			readerCollector.addReader(
					new OneToOneFetchReader( fetch, aliases, identifierReader, (EntityReference) fetchOwner )
			);
		}
		else {
			readerCollector.addReader(
					new EntityReferenceReader( fetch, aliases, identifierReader )
			);
		}

		//		3) and then readers for all fetches not part of the identifier
		processJoinFetches(
				selectStatementBuilder,
				factory,
				joinFragment,
				fetch,
				buildingParameters,
				aliasResolutionContext,
				readerCollector,
				stats
		);
	}

	/**
	 * Renders the pieces indicated by the incoming EntityFetch reference into the in-flight SQL statement builder.
	 *
	 * @param selectStatementBuilder The builder containing the in-flight SQL query definition.
	 * @param factory The SessionFactory SPI
	 * @param joinFragment The in-flight SQL JOIN definition.
	 * @param fetchOwner The owner of {@code fetch}
	 * @param fetch The fetch which indicates the information to be rendered.
	 * @param buildingParameters The settings/options for SQL building
	 * @param aliasResolutionContext The reference cache for entity/collection aliases
	 *
	 * @return The used aliases
	 */
	private static EntityReferenceAliases renderSqlFragments(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			FetchOwner fetchOwner,
			EntityFetch fetch,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext) {
		final EntityReferenceAliases aliases = aliasResolutionContext.resolveAliases( fetch );

		final String rhsAlias = aliases.getTableAlias();
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
						aliases.getColumnAliases().getSuffix(),
						null,
						true
				)
		);

		return aliases;
	}

	private static EntityIdentifierReader buildIdentifierReader(
			SelectStatementBuilder selectStatementBuilder,
			SessionFactoryImplementor factory,
			JoinFragment joinFragment,
			EntityReference entityReference,
			QueryBuildingParameters buildingParameters,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			EntityReferenceAliases aliases,
			FetchStatsImpl stats) {
		final List<EntityReferenceReader> identifierFetchReaders = new ArrayList<EntityReferenceReader>();
		final ReaderCollector identifierFetchReaderCollector = new ReaderCollector() {
			@Override
			public void addReader(CollectionReferenceReader collectionReferenceReader) {
				throw new IllegalStateException( "Identifier cannot contain collection fetches" );
			}

			@Override
			public void addReader(EntityReferenceReader entityReferenceReader) {
				identifierFetchReaders.add( entityReferenceReader );
			}
		};
		for ( Fetch fetch : entityReference.getIdentifierDescription().getFetches() ) {
			processFetch(
					selectStatementBuilder,
					factory,
					joinFragment,
					(FetchOwner) entityReference,
					fetch,
					buildingParameters,
					aliasResolutionContext,
					identifierFetchReaderCollector,
					stats
			);
		}
		return new EntityIdentifierReaderImpl(
				entityReference,
				aliases,
				identifierFetchReaders
		);
	}

	private static List<EntityReferenceReader> collectIdentifierFetchReaders(
			EntityReference entityReference,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector) {
		final Type identifierType = entityReference.getEntityPersister().getIdentifierType();
		if ( ! identifierType.isComponentType() ) {
			return Collections.emptyList();
		}

		final Fetch[] fetches = entityReference.getIdentifierDescription().getFetches();
		if ( fetches == null || fetches.length == 0 ) {
			return Collections.emptyList();
		}

		final List<EntityReferenceReader> readers = new ArrayList<EntityReferenceReader>();
		for ( Fetch fetch : fetches ) {
			collectIdentifierFetchReaders( aliasResolutionContext, readers, entityReference, fetch, readerCollector );
		}
		return readers;
	}


	private static void collectIdentifierFetchReaders(
			AliasResolutionContext aliasResolutionContext,
			List<EntityReferenceReader> readers,
			EntityReference entityReference,
			Fetch fetch,
			ReaderCollector readerCollector) {
		if ( CompositeFetch.class.isInstance( fetch ) ) {
			for ( Fetch subFetch : ( (CompositeFetch) fetch).getFetches() ) {
				collectIdentifierFetchReaders( aliasResolutionContext, readers, entityReference, subFetch, readerCollector );
			}
		}
		else if ( ! EntityReference.class.isInstance( fetch ) ) {
			throw new IllegalStateException(
					String.format(
							"Non-entity (and non-composite) fetch [%s] was found as part of entity identifier : %s",
							fetch,
							entityReference.getEntityPersister().getEntityName()
					)
			);
		}
		else {
			// todo : add a mapping here from EntityReference -> EntityReferenceReader
			//
			// need to be careful here about bi-directionality, just not sure how to best check for bi-directionality here.
			//
			final EntityReference fetchedEntityReference = (EntityReference) fetch;
			final EntityReferenceAliases fetchedAliases = aliasResolutionContext.resolveAliases( fetchedEntityReference );

			if ( BidirectionalEntityFetch.class.isInstance( fetchedEntityReference ) ) {
				return;
			}


			final EntityReferenceReader reader = new EntityReferenceReader(
					fetchedEntityReference,
					aliasResolutionContext.resolveAliases( fetchedEntityReference ),
					new EntityIdentifierReaderImpl(
							fetchedEntityReference,
							fetchedAliases,
							Collections.<EntityReferenceReader>emptyList()
					)
			);

			readerCollector.addReader( reader );
//			readers.add( reader );
		}
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
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			FetchStatsImpl stats) {
		stats.processingFetch( fetch );

		final CollectionReferenceAliases aliases = aliasResolutionContext.resolveAliases( fetch );

		if ( fetch.getCollectionPersister().isManyToMany() ) {
			final QueryableCollection queryableCollection = (QueryableCollection) fetch.getCollectionPersister();
			final Joinable joinableCollection = (Joinable) fetch.getCollectionPersister();

			// for many-to-many we have 3 table aliases.  By way of example, consider a normal m-n: User<->Role
			// where User is the FetchOwner and Role (User.roles) is the Fetch.  We'd have:
			//		1) the owner's table : user
			final String ownerTableAlias = resolveLhsTableAlias( fetchOwner, fetch, aliasResolutionContext );
			//		2) the m-n table : user_role
			final String collectionTableAlias = aliases.getCollectionTableAlias();
			//		3) the element table : role
			final String elementTableAlias = aliases.getElementTableAlias();

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
								aliases.getEntityElementColumnAliases().getSuffix(),
								aliases.getCollectionColumnAliases().getSuffix(),
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

			final EntityReference elementEntityReference = (EntityReference) fetch.getElementGraph();
			readerCollector.addReader(
					new EntityReferenceReader(
							elementEntityReference,
							entityReferenceAliases,
							buildIdentifierReader(
									selectStatementBuilder,
									factory,
									joinFragment,
									elementEntityReference,
									buildingParameters,
									aliasResolutionContext,
									readerCollector,
									entityReferenceAliases,
									stats
							)
					)
			);
		}
		else {
			final QueryableCollection queryableCollection = (QueryableCollection) fetch.getCollectionPersister();
			final Joinable joinableCollection = (Joinable) fetch.getCollectionPersister();

			final String rhsTableAlias = aliases.getElementTableAlias();
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

				final EntityReference elementEntityReference = (EntityReference) fetch.getElementGraph();
				readerCollector.addReader(
						new EntityReferenceReader(
								elementEntityReference,
								entityReferenceAliases,
								buildIdentifierReader(
										selectStatementBuilder,
										factory,
										joinFragment,
										elementEntityReference,
										buildingParameters,
										aliasResolutionContext,
										readerCollector,
										entityReferenceAliases,
										stats
								)
						)
				);
			}

			final String ordering = queryableCollection.getSQLOrderByString( rhsTableAlias );
			if ( StringHelper.isNotEmpty( ordering ) ) {
				selectStatementBuilder.appendOrderByFragment( ordering );
			}
		}

		readerCollector.addReader( new CollectionReferenceReader( fetch, aliases ) );
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
