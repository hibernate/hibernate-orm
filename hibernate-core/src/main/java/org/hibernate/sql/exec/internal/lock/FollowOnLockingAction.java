/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;

/**
 * PostAction for a {@linkplain org.hibernate.sql.exec.internal.JdbcSelectWithActions} which
 * performs follow-on locking based on the loaded values.
 *
 * @implSpec Relies on the fact that {@linkplain LoadedValuesCollector} has
 * already applied filtering for things which actually need locked.
 *
 * @author Steve Ebersole
 */
public class FollowOnLockingAction implements PostAction {
	private final LoadedValuesCollectorImpl loadedValuesCollector;
	private final LockMode lockMode;
	private final Timeout lockTimeout;
	private final Locking.Scope lockScope;

	private FollowOnLockingAction(
			LoadedValuesCollectorImpl loadedValuesCollector,
			LockMode lockMode,
			Timeout lockTimeout,
			Locking.Scope lockScope) {
		this.loadedValuesCollector = loadedValuesCollector;
		this.lockMode = lockMode;
		this.lockTimeout = lockTimeout;
		this.lockScope = lockScope;
	}

	public static void apply(
			LockOptions lockOptions,
			QuerySpec lockingTarget,
			LockingClauseStrategy lockingClauseStrategy,
			JdbcSelectWithActions.Builder jdbcSelectBuilder) {
		final var fromClause = lockingTarget.getFromClause();
		final var loadedValuesCollector = resolveLoadedValuesCollector( fromClause, lockingClauseStrategy );

		// NOTE: we need to set this separately so that it can get incorporated into
		// the JdbcValuesSourceProcessingState for proper callbacks
		jdbcSelectBuilder.setLoadedValuesCollector( loadedValuesCollector );

		// additionally, add a post-action which uses the collected values.
		jdbcSelectBuilder.appendPostAction( new FollowOnLockingAction(
				loadedValuesCollector,
				lockOptions.getLockMode(),
				lockOptions.getTimeout(),
				lockOptions.getScope()
		) );
	}

	@Override
	public void performPostAction(
			StatementAccess jdbcStatementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		LockingHelper.logLoadedValues( loadedValuesCollector );

		final var session = executionContext.getSession();

		// NOTE: we deal with effective graphs here to make sure embedded associations are treated as lazy
		final var effectiveEntityGraph = session.getLoadQueryInfluencers().getEffectiveEntityGraph();
		final var initialGraph = effectiveEntityGraph.getGraph();
		final var initialSemantic = effectiveEntityGraph.getSemantic();

		try {
			// collect registrations by entity type
			final var entitySegments = segmentLoadedValues();
			final Map<EntityMappingType, Map<PluralAttributeMapping, List<CollectionKey>>> collectionSegments =
					lockScope == Locking.Scope.INCLUDE_FETCHES
							? segmentLoadedCollections()
							: emptyMap();

			// for each entity-type, prepare a locking select statement per table.
			// this is based on the attributes for "state array" ordering purposes -
			// we match each attribute to the table it is mapped to and add it to
			// the select-list for that table-segment.
			entitySegments.forEach( (entityMappingType, entityKeys) -> {
				if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
					SQL_EXEC_LOGGER.debugf( "Starting follow-on locking process - %s", entityMappingType.getEntityName() );
				}

				// apply an empty "fetch graph" to make sure any embedded associations reachable from
				// any of the DomainResults we will create are treated as lazy
				final var graph = entityMappingType.createRootGraph( session );
				effectiveEntityGraph.clear();
				effectiveEntityGraph.applyGraph( graph, GraphSemantic.FETCH );

				// create a table-lock reference for each table for the entity (keyed by name)
				final var tableLocks = prepareTableLocks( entityMappingType, entityKeys, session );

				// create a cross-reference of information related to an entity based on its identifier,
				// we'll use this later when we adjust the state array and inject state into the entity instance.
				final var entityDetailsMap = LockingHelper.resolveEntityKeys( entityKeys, executionContext );

				entityMappingType.forEachAttributeMapping( (index, attributeMapping) -> {
					// we need to handle collections specially (which we do below, so skip them here)
					if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
						final var tableLock = resolveTableLock( attributeMapping, tableLocks, entityMappingType );
						if ( tableLock == null ) {
							throw new AssertionFailure( String.format(
									Locale.ROOT,
									"Unable to locate table for attribute `%s`",
									attributeMapping.getNavigableRole().getFullPath()
							) );
						}

						// here we apply the selection for the attribute to the corresponding table-lock ref
						tableLock.applyAttribute( index, attributeMapping );
					}
				} );

				// now we do process any collections, if asked
				if ( lockScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
					SqmMutationStrategyHelper.visitCollectionTables( entityMappingType, (attribute) -> {
						// we may need to lock the "collection table".
						// the conditions are a bit unclear as to directionality, etc., so for now lock each.
						LockingHelper.lockCollectionTable(
								attribute,
								lockMode,
								lockTimeout,
								entityDetailsMap,
								executionContext
						);
					} );
				}
				else if ( lockScope == Locking.Scope.INCLUDE_FETCHES
						&& loadedValuesCollector.getCollectedCollections() != null
						&& !loadedValuesCollector.getCollectedCollections().isEmpty() ) {
					final var attributeKeys = collectionSegments.get( entityMappingType );
					if ( attributeKeys != null ) {
						for ( var entry : attributeKeys.entrySet() ) {
							LockingHelper.lockCollectionTable(
									entry.getKey(),
									lockMode,
									lockTimeout,
									entry.getValue(),
									executionContext
							);
						}
					}
				}


				// at this point, we have all the individual locking selects ready to go - execute them
				final var lockingOptions = buildLockingOptions( executionContext );
				tableLocks.forEach( (s, tableLock) ->
						tableLock.performActions( entityDetailsMap, lockingOptions, session ) );
			} );
		}
		finally {
			// reset the effective graph to whatever it was when we started
			effectiveEntityGraph.clear();
			session.getLoadQueryInfluencers().applyEntityGraph( initialGraph, initialSemantic );
		}
	}

	private TableLock resolveTableLock(
			AttributeMapping attributeMapping,
			Map<String, TableLock> tableSegments,
			EntityMappingType entityMappingType) {
		final Object key =
				entityMappingType.getEntityPersister() instanceof UnionSubclassEntityPersister usp
						// In the union-subclass strategy, attributes defined on the
						// super are reported as contained by the logical super table.
						// See also the hacks in TableSegment to deal with this.
						// todo (JdbcOperation) : need to allow for secondary-tables
						? usp.getMappedTableDetails().getTableName()
						: attributeMapping.getContainingTableExpression();
		return tableSegments.get( key );
	}

	private QueryOptions buildLockingOptions(ExecutionContext executionContext) {
		final var lockingQueryOptions = new QueryOptionsImpl();
		lockingQueryOptions.getLockOptions().setLockMode( lockMode );
		lockingQueryOptions.getLockOptions().setTimeout( lockTimeout );
		lockingQueryOptions.getLockOptions().setFollowOnStrategy( Locking.FollowOn.DISALLOW );
		if ( executionContext.getQueryOptions().isReadOnly() == Boolean.TRUE ) {
			lockingQueryOptions.setReadOnly( true );
		}
		return lockingQueryOptions;
	}

	private Map<EntityMappingType, List<EntityKey>> segmentLoadedValues() {
		final Map<EntityMappingType, List<EntityKey>> map = new IdentityHashMap<>();
		LockingHelper.segmentLoadedValues( loadedValuesCollector.getCollectedRootEntities(), map );
		LockingHelper.segmentLoadedValues( loadedValuesCollector.getCollectedNonRootEntities(), map );
		if ( map.isEmpty() ) {
			throw new AssertionFailure( "Expecting some values" );
		}
		return map;
	}

	private Map<EntityMappingType, Map<PluralAttributeMapping, List<CollectionKey>>> segmentLoadedCollections() {
		final Map<EntityMappingType, Map<PluralAttributeMapping, List<CollectionKey>>> map = new HashMap<>();
		LockingHelper.segmentLoadedCollections( loadedValuesCollector.getCollectedCollections(), map );
		return map;
	}

	private Map<String, TableLock> prepareTableLocks(
			EntityMappingType entityMappingType,
			List<EntityKey> entityKeys,
			SharedSessionContractImplementor session) {
		final Map<String, TableLock> segments = new HashMap<>();
		entityMappingType.forEachTableDetails( (tableDetails) -> segments.put(
				tableDetails.getTableName(),
				createTableLock( tableDetails, entityMappingType, entityKeys, session )
		) );
		return segments;
	}

	private TableLock createTableLock(TableDetails tableDetails, EntityMappingType entityMappingType, List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		return new TableLock( tableDetails, entityMappingType, entityKeys, session );
	}

	private static LoadedValuesCollectorImpl resolveLoadedValuesCollector(
			FromClause fromClause,
			LockingClauseStrategy lockingClauseStrategy) {
		final var fromClauseRoots = fromClause.getRoots();
		if ( fromClauseRoots.size() == 1 ) {
			return new LoadedValuesCollectorImpl(
					List.of( fromClauseRoots.get( 0 ).getNavigablePath() ),
					lockingClauseStrategy
			);
		}
		else {
			return new LoadedValuesCollectorImpl(
					fromClauseRoots.stream().map( TableGroup::getNavigablePath ).toList(),
					lockingClauseStrategy
			);
		}
	}

	public static class LoadedValuesCollectorImpl implements LoadedValuesCollector {
		private final List<NavigablePath> rootPaths;
		private final Collection<NavigablePath> pathsToLock;

		private List<LoadedEntityRegistration> rootEntitiesToLock;
		private List<LoadedEntityRegistration> nonRootEntitiesToLock;
		private List<LoadedCollectionRegistration> collectionsToLock;

		public LoadedValuesCollectorImpl(List<NavigablePath> rootPaths, LockingClauseStrategy lockingClauseStrategy) {
			this.rootPaths = rootPaths;
			pathsToLock = LockingHelper.extractPathsToLock( lockingClauseStrategy );
		}

		@Override
		public void registerEntity(NavigablePath navigablePath, EntityMappingType entityDescriptor, EntityKey entityKey) {
			if ( pathsToLock.contains( navigablePath ) ) {
				if ( rootPaths.contains( navigablePath ) ) {
					if ( rootEntitiesToLock == null ) {
						rootEntitiesToLock = new ArrayList<>();
					}
					rootEntitiesToLock.add(
							new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
				}
				else {
					if ( nonRootEntitiesToLock == null ) {
						nonRootEntitiesToLock = new ArrayList<>();
					}
					nonRootEntitiesToLock.add(
							new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
				}
			}
		}

		@Override
		public void registerCollection(NavigablePath navigablePath, PluralAttributeMapping collectionDescriptor, CollectionKey collectionKey) {
			if ( pathsToLock.contains( navigablePath ) ) {
				if ( collectionsToLock == null ) {
					collectionsToLock = new ArrayList<>();
				}
				collectionsToLock.add(
						new LoadedCollectionRegistration( navigablePath, collectionDescriptor, collectionKey ) );
			}
		}

		@Override
		public void clear() {
			if ( rootEntitiesToLock != null ) {
				rootEntitiesToLock.clear();
			}
			if ( nonRootEntitiesToLock != null ) {
				nonRootEntitiesToLock.clear();
			}
			if ( collectionsToLock != null ) {
				collectionsToLock.clear();
			}
		}

		@Override
		public List<LoadedEntityRegistration> getCollectedRootEntities() {
			return rootEntitiesToLock;
		}

		@Override
		public List<LoadedEntityRegistration> getCollectedNonRootEntities() {
			return nonRootEntitiesToLock;
		}

		@Override
		public List<LoadedCollectionRegistration> getCollectedCollections() {
			return collectionsToLock;
		}
	}
}
