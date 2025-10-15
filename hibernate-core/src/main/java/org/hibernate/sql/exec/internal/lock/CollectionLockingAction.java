/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.StatementAccess;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;

/**
 * PostAction intended to perform collection locking with
 * {@linkplain Locking.Scope#INCLUDE_COLLECTIONS} for Dialects
 * which support "table hint locking" (T-SQL variants).
 *
 * @author Steve Ebersole
 */
public class CollectionLockingAction implements PostAction {
	private final LoadedValuesCollectorImpl loadedValuesCollector;
	private final LockMode lockMode;
	private final Timeout lockTimeout;

	private CollectionLockingAction(
			LoadedValuesCollectorImpl loadedValuesCollector,
			LockMode lockMode,
			Timeout lockTimeout) {
		this.loadedValuesCollector = loadedValuesCollector;
		this.lockMode = lockMode;
		this.lockTimeout = lockTimeout;
	}

	public static void apply(
			LockOptions lockOptions,
			QuerySpec lockingTarget,
			JdbcSelectWithActions.Builder jdbcSelectBuilder) {
		assert lockOptions.getScope() == Locking.Scope.INCLUDE_COLLECTIONS;

		final var loadedValuesCollector = resolveLoadedValuesCollector( lockingTarget.getFromClause() );

		// NOTE: we need to set this separately so that it can get incorporated into
		// the JdbcValuesSourceProcessingState for proper callbacks
		jdbcSelectBuilder.setLoadedValuesCollector( loadedValuesCollector );

		// additionally, add a post-action which uses the collected values.
		jdbcSelectBuilder.appendPostAction( new CollectionLockingAction(
				loadedValuesCollector,
				lockOptions.getLockMode(),
				lockOptions.getTimeout()
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

		// collect registrations by entity type
		final var entitySegments = segmentLoadedValues( loadedValuesCollector );

		try {
			// for each entity-type, prepare a locking select statement per table.
			// this is based on the attributes for "state array" ordering purposes -
			// we match each attribute to the table it is mapped to and add it to
			// the select-list for that table-segment.
			entitySegments.forEach( (entityMappingType, entityKeys) -> {
				if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
					SQL_EXEC_LOGGER.startingIncludeCollectionsLockingProcess( entityMappingType.getEntityName() );
				}

				// apply an empty "fetch graph" to make sure any embedded associations reachable from
				// any of the DomainResults we will create are treated as lazy
				final var graph = entityMappingType.createRootGraph( session );
				effectiveEntityGraph.clear();
				effectiveEntityGraph.applyGraph( graph, GraphSemantic.FETCH );

				// create a cross-reference of information related to an entity based on its identifier.
				// we use this as the collection owners whose collections need to be locked
				final var entityDetailsMap = LockingHelper.resolveEntityKeys( entityKeys, executionContext );

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
			} );
		}
		finally {
			// reset the effective graph to whatever it was when we started
			effectiveEntityGraph.clear();
			session.getLoadQueryInfluencers().applyEntityGraph( initialGraph, initialSemantic );
		}
	}

	private static LoadedValuesCollectorImpl resolveLoadedValuesCollector(FromClause fromClause) {
		final var fromClauseRoots = fromClause.getRoots();
		if ( fromClauseRoots.size() == 1 ) {
			return new LoadedValuesCollectorImpl(
					List.of( fromClauseRoots.get( 0 ).getNavigablePath() )
			);
		}
		else {
			return new LoadedValuesCollectorImpl(
					fromClauseRoots.stream().map( TableGroup::getNavigablePath ).toList()
			);
		}
	}

	private static Map<EntityMappingType, List<EntityKey>> segmentLoadedValues(LoadedValuesCollector loadedValuesCollector) {
		final Map<EntityMappingType, List<EntityKey>> map = new IdentityHashMap<>();
		LockingHelper.segmentLoadedValues( loadedValuesCollector.getCollectedRootEntities(), map );
		LockingHelper.segmentLoadedValues( loadedValuesCollector.getCollectedNonRootEntities(), map );
		if ( map.isEmpty() ) {
			// NOTE: this may happen with Session#lock routed through SqlAstBasedLockingStrategy.
			// however, we cannot tell that is the code path from here.
		}
		return map;
	}

	private static class LoadedValuesCollectorImpl implements LoadedValuesCollector {
		private final List<NavigablePath> rootPaths;

		private List<LoadedEntityRegistration> rootEntitiesToLock;
		private List<LoadedEntityRegistration> nonRootEntitiesToLock;
		private List<LoadedCollectionRegistration> collectionsToLock;

		private LoadedValuesCollectorImpl(List<NavigablePath> rootPaths) {
			this.rootPaths = rootPaths;
		}

		@Override
		public void registerEntity(NavigablePath navigablePath, EntityMappingType entityDescriptor, EntityKey entityKey) {
			if ( rootPaths.contains( navigablePath ) ) {
				if ( rootEntitiesToLock == null ) {
					rootEntitiesToLock = new ArrayList<>();
				}
				rootEntitiesToLock.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
			}
			else {
				if ( nonRootEntitiesToLock == null ) {
					nonRootEntitiesToLock = new ArrayList<>();
				}
				nonRootEntitiesToLock.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
			}
		}

		@Override
		public void registerCollection(NavigablePath navigablePath, PluralAttributeMapping collectionDescriptor, CollectionKey collectionKey) {
			if ( collectionsToLock == null ) {
				collectionsToLock = new ArrayList<>();
			}
			collectionsToLock.add( new LoadedCollectionRegistration( navigablePath, collectionDescriptor, collectionKey ) );
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
