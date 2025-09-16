/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.LoadedValuesCollector.LoadedEntityRegistration;
import org.hibernate.sql.exec.spi.PostAction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
	private final LoadedValuesCollector loadedValuesCollector;
	private final LockMode lockMode;
	private final Timeout lockTimeout;
	private final Locking.Scope lockScope;

	public FollowOnLockingAction(
			LoadedValuesCollector loadedValuesCollector,
			LockMode lockMode,
			Timeout lockTimeout,
			Locking.Scope lockScope) {
		this.loadedValuesCollector = loadedValuesCollector;
		this.lockMode = lockMode;
		this.lockTimeout = lockTimeout;
		this.lockScope = lockScope;
	}

	@Override
	public void performPostAction(
			StatementAccess jdbcStatementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		logLoadedValues( loadedValuesCollector );

		final SharedSessionContractImplementor session = executionContext.getSession();

		// NOTE: we deal with effective graphs here to make sure associations are treated as lazy
		final EffectiveEntityGraph effectiveEntityGraph = session.getLoadQueryInfluencers().getEffectiveEntityGraph();
		final RootGraphImplementor<?> initialGraph = effectiveEntityGraph.getGraph();
		final GraphSemantic initialSemantic = effectiveEntityGraph.getSemantic();

		try {
			// collect registrations by entity type
			final Map<EntityMappingType, List<EntityKey>> entitySegments = segmentLoadedValues();

			// for each entity-type, prepare a locking select statement per table.
			// this is based on the attributes for "state array" ordering purposes -
			// we match each attribute to the table it is mapped to and add it to
			// the select-list for that table-segment.
			entitySegments.forEach( (entityMappingType, entityKeys) -> {
				if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
					SQL_EXEC_LOGGER.debugf( "Starting follow-on locking process - %s", entityMappingType.getEntityName() );
				}

				// apply an empty "fetch graph" to make sure any associations reachable from
				// any of the DomainResults we will create are treated as lazy
				final RootGraphImplementor<?> graph = entityMappingType.createRootGraph( session );
				effectiveEntityGraph.clear();
				effectiveEntityGraph.applyGraph( graph, GraphSemantic.FETCH );

				// create a segment for each table for the entity (keyed by name)
				final Map<String, TableSegment> tableSegments = prepareTableSegments( entityMappingType, entityKeys, session );

				// create a cross-reference of information related to an entity based on its identifier,
				// we'll use this later when we adjust the state array and inject state into the entity instance.
				final Map<Object, EntityDetails> entityDetailsMap = resolveEntityKeys( entityKeys, executionContext );

				entityMappingType.forEachAttributeMapping( (index, attributeMapping) -> {
					if ( attributeMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
						// we need to handle collections specially (which we do below, so skip them here)
						return;
					}

					final String tableExpression = attributeMapping.getContainingTableExpression();
					final TableSegment entityTableSegment = tableSegments.get( tableExpression );

					// here we apply the selection for the attribute to the corresponding
					// table-segment keeping track of the state array index for later.
					entityTableSegment.applyAttribute( index, attributeMapping );
				} );

				// now we do process any collections, if asked
				if ( lockScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
					SqmMutationStrategyHelper.visitCollectionTables( entityMappingType, (attribute) -> {
						// we may need to lock the "collection table".
						// the conditions are a bit unclear, so for now always lock them.
						CollectionTableHelper.lockCollectionTable(
								attribute,
								lockMode,
								lockTimeout,
								entityDetailsMap,
								executionContext
						);
					} );
				}


				// at this point, we have all the individual locking selects ready to go.
				// 		execute them and process the results against `entityDetailsMap` -
				// 		for each row updating "loaded state" and ultimately refreshing the
				// 		entity instance state

				// NOTE: share the context between table-segments (perf)
				final ExecutionContext lockingExecutionContext = buildLockingExecutionContext( session );
				tableSegments.forEach( (s, entityTableSegment) -> {
					entityTableSegment.performActions( entityDetailsMap, lockingExecutionContext );
				} );
			} );
		}
		finally {
			// reset the effective graph to whatever it was when we started
			effectiveEntityGraph.clear();
			session.getLoadQueryInfluencers().applyEntityGraph( initialGraph, initialSemantic );
		}
	}

	@SuppressWarnings("removal")
	private ExecutionContext buildLockingExecutionContext(SharedSessionContractImplementor session) {
		final QueryOptionsImpl lockingQueryOptions = new QueryOptionsImpl();
		lockingQueryOptions.getLockOptions().setLockMode( lockMode );
		lockingQueryOptions.getLockOptions().setTimeout( lockTimeout );
		return new BaseExecutionContext( session ) {
			@Override
			public QueryOptions getQueryOptions() {
				return lockingQueryOptions;
			}
		};
	}

	/**
	 * Collect loaded entities by type
	 */
	private Map<EntityMappingType, List<EntityKey>> segmentLoadedValues() {
		final Map<EntityMappingType, List<EntityKey>> map = new IdentityHashMap<>();
		segmentLoadedValues( loadedValuesCollector.getCollectedRootEntities(), map );
		segmentLoadedValues( loadedValuesCollector.getCollectedNonRootEntities(), map );
		if ( map.isEmpty() ) {
			throw new AssertionFailure( "Expecting some values" );
		}
		return map;
	}

	private void segmentLoadedValues(List<LoadedEntityRegistration> registrations, Map<EntityMappingType, List<EntityKey>> map) {
		if ( registrations == null ) {
			return;
		}

		registrations.forEach( (registration) -> {
			final List<EntityKey> entityKeys = map.computeIfAbsent(
					registration.entityDescriptor(),
					entityMappingType -> new ArrayList<>()
			);
			entityKeys.add( registration.entityKey() );
		} );
	}

	private Map<String, TableSegment> prepareTableSegments(
			EntityMappingType entityMappingType,
			List<EntityKey> entityKeys,
			SharedSessionContractImplementor session) {
		final Map<String, TableSegment> segments = new HashMap<>();
		entityMappingType.forEachTableDetails( (tableDetails) -> segments.put(
				tableDetails.getTableName(),
				new TableSegment( tableDetails, entityMappingType, entityKeys, session )
		) );
		return segments;
	}

	private Map<Object, EntityDetails> resolveEntityKeys(List<EntityKey> entityKeys, ExecutionContext executionContext) {
		final Map<Object, EntityDetails> map = new HashMap<>();
		final PersistenceContext persistenceContext = executionContext.getSession().getPersistenceContext();
		entityKeys.forEach( (entityKey) -> {
			final Object instance = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( instance );
			map.put( entityKey.getIdentifierValue(), new EntityDetails( entityKey, entry, instance ) );
		} );
		return map;
	}

	public static void logLoadedValues(LoadedValuesCollector collector) {
		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.debug( "Follow-on locking collected loaded values..." );

			SQL_EXEC_LOGGER.debug( "  Loaded root entities:" );
			collector.getCollectedRootEntities().forEach( (reg) -> {
				SQL_EXEC_LOGGER.debugf( "    - %s#%s", reg.entityDescriptor().getEntityName(), reg.entityKey().getIdentifier() );
			} );

			SQL_EXEC_LOGGER.debug( "  Loaded non-root entities:" );
			collector.getCollectedNonRootEntities().forEach( (reg) -> {
				SQL_EXEC_LOGGER.debugf( "    - %s#%s", reg.entityDescriptor().getEntityName(), reg.entityKey().getIdentifier() );
			} );

			SQL_EXEC_LOGGER.debug( "  Loaded collections:" );
			collector.getCollectedCollections().forEach( (reg) -> {
				SQL_EXEC_LOGGER.debugf( "    - %s#%s", reg.collectionDescriptor().getRootPathName(), reg.collectionKey().getKey() );
			} );
		}
	}

}
