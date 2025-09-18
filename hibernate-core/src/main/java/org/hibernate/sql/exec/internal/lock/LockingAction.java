/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
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
import java.util.Locale;
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
public class LockingAction implements PostAction {
	private final LoadedValuesCollector loadedValuesCollector;
	private final LockMode lockMode;
	private final Timeout lockTimeout;
	private final Locking.Scope lockScope;

	public LockingAction(
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
		LockingHelper.logLoadedValues( loadedValuesCollector );

		final SharedSessionContractImplementor session = executionContext.getSession();

		// NOTE: we deal with effective graphs here to make sure embedded associations are treated as lazy
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

				// apply an empty "fetch graph" to make sure any embedded associations reachable from
				// any of the DomainResults we will create are treated as lazy
				final RootGraphImplementor<?> graph = entityMappingType.createRootGraph( session );
				effectiveEntityGraph.clear();
				effectiveEntityGraph.applyGraph( graph, GraphSemantic.FETCH );

				// create a table-lock reference for each table for the entity (keyed by name)
				final Map<String, TableLock> tableLocks = prepareTableLocks( entityMappingType, entityKeys, session );

				// create a cross-reference of information related to an entity based on its identifier,
				// we'll use this later when we adjust the state array and inject state into the entity instance.
				final Map<Object, EntityDetails> entityDetailsMap = resolveEntityKeys( entityKeys, executionContext );

				entityMappingType.forEachAttributeMapping( (index, attributeMapping) -> {
					if ( attributeMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
						// we need to handle collections specially (which we do below, so skip them here)
						return;
					}

					final TableLock tableLock = resolveTableLock( attributeMapping, tableLocks, entityMappingType );

					if ( tableLock == null ) {
						throw new AssertionFailure( String.format(
								Locale.ROOT,
								"Unable to locate table for attribute `%s`",
								attributeMapping.getNavigableRole().getFullPath()
						) );
					}

					// here we apply the selection for the attribute to the corresponding table-lock ref
					tableLock.applyAttribute( index, attributeMapping );
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


				// at this point, we have all the individual locking selects ready to go - execute them
				final QueryOptions lockingOptions = buildLockingOptions( executionContext );
				tableLocks.forEach( (s, tableLock) -> {
					tableLock.performActions( entityDetailsMap, lockingOptions, session );
				} );
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
		if ( entityMappingType.getEntityPersister() instanceof UnionSubclassEntityPersister usp ) {
			// in the union-subclass strategy, attributes defined on the super are reported as
			// contained by the logical super table.  See also the hacks in TableSegment
			// to deal with this
			// todo (JdbcOperation) : need to allow for secondary-tables
			return tableSegments.get( usp.getMappedTableDetails().getTableName() );
		}
		else {
			return tableSegments.get( attributeMapping.getContainingTableExpression() );
		}
	}

	private QueryOptions buildLockingOptions(ExecutionContext executionContext) {
		final QueryOptionsImpl lockingQueryOptions = new QueryOptionsImpl();
		lockingQueryOptions.getLockOptions().setLockMode( lockMode );
		lockingQueryOptions.getLockOptions().setTimeout( Timeouts.WAIT_FOREVER );
		lockingQueryOptions.getLockOptions().setFollowOnStrategy( Locking.FollowOn.DISALLOW );
		if ( executionContext.getQueryOptions().isReadOnly() == Boolean.TRUE ) {
			lockingQueryOptions.setReadOnly( true );
		}
		return lockingQueryOptions;
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

}
