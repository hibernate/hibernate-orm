/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.ops.spi.DatabaseSelect;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;
import org.hibernate.sql.ops.spi.LoadedValuesCollector.LoadedEntityRegistration;
import org.hibernate.sql.ops.spi.PostAction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.sql.ops.internal.DatabaseOperationLogging.DB_OP_DEBUG_ENABLED;
import static org.hibernate.sql.ops.internal.DatabaseOperationLogging.DB_OP_LOGGER;

/**
 * PostAction for a @{@linkplain DatabaseSelect} which performs follow-on locking
 * based on the loaded values.
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

	public FollowOnLockingAction(
			LoadedValuesCollector loadedValuesCollector,
			LockMode lockMode,
			Timeout lockTimeout) {
		this.loadedValuesCollector = loadedValuesCollector;
		this.lockMode = lockMode;
		this.lockTimeout = lockTimeout;
	}

	@Override
	public void performPostAction(
			StatementAccess jdbcStatementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		logLoadedValues( loadedValuesCollector );

		final SharedSessionContractImplementor session = executionContext.getSession();

		// collect registrations by entity type
		final Map<EntityMappingType, List<EntityKey>> entitySegments = segmentLoadedValues();

		// for each entity-type, prepare a locking select statement per table.
		// this is based on the attributes for "state array" ordering purposes -
		// we match each attribute to the table it is mapped to and add it to
		// the select-list for that table-segment.
		entitySegments.forEach( (entityMappingType, entityKeys) -> {
			if ( DB_OP_DEBUG_ENABLED ) {
				DB_OP_LOGGER.debugf( "Starting follow-on locking process - %s", entityMappingType.getEntityName() );
			}

			// create a segment for each table for the entity
			final Map<String,TableSegment> tableSegments = prepareTableSegments( entityMappingType, entityKeys, session );

			// create a cross-reference of information related to an entity based on its identifier,
			// we'll use this later when we adjust the state array and inject state into the entity instance.
			final Map<Object, EntityDetails> entityDetailsMap = resolveEntityKeys( entityKeys, executionContext );

			entityMappingType.forEachAttributeMapping( (index, attributeMapping) -> {
				if ( attributeMapping.isPluralAttributeMapping() ) {
					// for now...
					return;
				}
				final String tableExpression = attributeMapping.getContainingTableExpression();
				final TableSegment tableSegment = tableSegments.get( tableExpression );

				// here we apply the selection for the attribute to the corresponding
				// table-segment keeping track of the state array index for later.
				tableSegment.applyDomainResult( index, attributeMapping );
			} );

			// at this point, we have all the individual locking selects ready to go.
			// 		execute them and process the results against `entityDetailsMap` -
			// 		for each row updating "loaded state" and ultimately refreshing the
			// 		entity instance state

			// NOTE: we do this here to share the instance between table-segments
			final QueryOptionsImpl lockingQueryOptions = new QueryOptionsImpl();
			lockingQueryOptions.getLockOptions().setLockMode( lockMode );
			lockingQueryOptions.getLockOptions().setTimeout( lockTimeout );
			final ExecutionContext lockingExecutionContext = new BaseExecutionContext( session ) {
				@Override
				public QueryOptions getQueryOptions() {
					return lockingQueryOptions;
				}
			};

			tableSegments.forEach( (s, tableSegment) -> {
				tableSegment.performActions( entityDetailsMap, lockingExecutionContext );
			} );
		} );
	}

	/**
	 * Collect loaded entities by type
	 */
	private Map<EntityMappingType, List<EntityKey>> segmentLoadedValues() {
		final Map<EntityMappingType, List<EntityKey>> map = new IdentityHashMap<>();
		segmentLoadedValues( loadedValuesCollector.getCollectedRootEntities(), map );
		segmentLoadedValues( loadedValuesCollector.getCollectedNonRootEntities(), map );
		return map;
	}

	private void segmentLoadedValues(List<LoadedEntityRegistration> registrations, Map<EntityMappingType, List<EntityKey>> map) {
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
		final Map<Object, EntityDetails> map = new IdentityHashMap<>();
		final PersistenceContext persistenceContext = executionContext.getSession().getPersistenceContext();
		entityKeys.forEach( (entityKey) -> {
			final Object instance = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( instance );
			map.put( entityKey.getIdentifierValue(), new EntityDetails( entityKey, entry, instance ) );
		} );
		return map;
	}

	public static void logLoadedValues(LoadedValuesCollector collector) {
		if ( DB_OP_DEBUG_ENABLED ) {
			DB_OP_LOGGER.debug( "Follow-on locking collected loaded values..." );

			DB_OP_LOGGER.debug( "  Loaded root entities:" );
			collector.getCollectedRootEntities().forEach( (reg) -> {
				DB_OP_LOGGER.debugf( "    - %s#%s", reg.entityDescriptor().getEntityName(), reg.entityKey().getIdentifier() );
			} );

			DB_OP_LOGGER.debug( "  Loaded non-root entities:" );
			collector.getCollectedNonRootEntities().forEach( (reg) -> {
				DB_OP_LOGGER.debugf( "    - %s#%s", reg.entityDescriptor().getEntityName(), reg.entityKey().getIdentifier() );
			} );

			DB_OP_LOGGER.debug( "  Loaded collections:" );
			collector.getCollectedCollections().forEach( (reg) -> {
				DB_OP_LOGGER.debugf( "    - %s#%s", reg.collectionDescriptor().getRootPathName(), reg.collectionKey().getKey() );
			} );
		}
	}

}
