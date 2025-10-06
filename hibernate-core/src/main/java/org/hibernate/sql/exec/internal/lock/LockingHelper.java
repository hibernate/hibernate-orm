/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;

/**
 * Helper for dealing with follow-on locking for collection-tables.
 *
 * @author Steve Ebersole
 */
public class LockingHelper {
	/**
	 * Lock a collection-table for a single entity.
	 *
	 * @param attributeMapping The plural attribute whose table needs locked.
	 * @param lockMode The lock mode to apply
	 * @param lockTimeout A lock timeout to apply, if one.
	 * @param collectionToLock The collection to lock.
	 *
	 * @see Session#lock
	 */
	public static void lockCollectionTable(
			PluralAttributeMapping attributeMapping,
			LockMode lockMode,
			Timeout lockTimeout,
			PersistentCollection<?> collectionToLock,
			ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final String keyTableName = keyDescriptor.getKeyTable();

		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.collectionLockingForCollectionTable( keyTableName, attributeMapping.getRootPathName() );
		}

		final var querySpec = new QuerySpec( true );

		final var tableReference = new NamedTableReference( keyTableName, "tbl" );

		querySpec.getFromClause()
				.addRoot( new LockingTableGroup(
						tableReference,
						keyTableName,
						attributeMapping,
						keyDescriptor.getKeySide().getModelPart()
				) );

		final var keyPart = keyDescriptor.getKeyPart();
		final var columnReference = new ColumnReference( tableReference, keyPart.getSelectable( 0 ) );

		// NOTE: We add the key column to the selection list, but never create a DomainResult
		// as we won't read the value back.  Ideally, we would read the "value column(s)" and
		// update the collection state accordingly much like is done for entity state -
		// however, the concern is minor, so for simplicity we do not.
		querySpec.getSelectClause()
				.addSqlSelection( new SqlSelectionImpl( columnReference, 0 ) );

		final int jdbcTypeCount = keyDescriptor.getJdbcTypeCount();
		final var parameterBindings = new JdbcParameterBindingsImpl( jdbcTypeCount );

		final ComparisonPredicate restriction;
		if ( jdbcTypeCount == 1 ) {
			final var jdbcParameter =
					new JdbcParameterImpl( keyPart.getSelectable( 0 ).getJdbcMapping() );
			keyDescriptor.breakDownJdbcValues(
					collectionToLock.getKey(),
					(valueIndex, value, jdbcValueMapping) -> {
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), value ) );
					},
					session
			);
			restriction = new ComparisonPredicate( columnReference, ComparisonOperator.EQUAL, jdbcParameter );
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			final List<JdbcParameter> jdbcParameters = new ArrayList<>( jdbcTypeCount );
			keyDescriptor.breakDownJdbcValues(
					collectionToLock.getKey(),
					(valueIndex, value, jdbcValueMapping) -> {
						columnReferences.add( new ColumnReference( tableReference, jdbcValueMapping ) );
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						jdbcParameters.add( jdbcParameter );
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
			final var columns = new SqlTuple( columnReferences, keyDescriptor );
			final var parameters = new SqlTuple( jdbcParameters, keyDescriptor );
			restriction = new ComparisonPredicate( columns, ComparisonOperator.EQUAL, parameters );
		}
		querySpec.applyPredicate( restriction );

		performLocking( querySpec, parameterBindings,
				lockingExecutionContext( lockMode, lockTimeout, executionContext ) );
	}

	/**
	 * Lock a collection-table.
	 *
	 * @param attributeMapping The plural attribute whose table needs locked.
	 * @param lockMode The lock mode to apply
	 * @param lockTimeout A lock timeout to apply, if one.
	 * @param ownerDetailsMap Details for each owner, whose collection-table rows should be locked.
	 */
public static void lockCollectionTable(
			PluralAttributeMapping attributeMapping,
			LockMode lockMode,
			Timeout lockTimeout,
			Map<Object, EntityDetails> ownerDetailsMap,
			ExecutionContext executionContext) {
		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final String keyTableName = keyDescriptor.getKeyTable();

		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.followOnLockingForCollectionTable( keyTableName, attributeMapping.getRootPathName() );
		}

		final var querySpec = new QuerySpec( true );

		final var tableReference = new NamedTableReference( keyTableName, "tbl" );

		querySpec.getFromClause()
				.addRoot( new LockingTableGroup(
						tableReference,
						keyTableName,
						attributeMapping,
						keyDescriptor.getKeySide().getModelPart()
				) );

		final var keyPart = keyDescriptor.getKeyPart();
		final var columnReference = new ColumnReference( tableReference, keyPart.getSelectable( 0 ) );

		// NOTE: We add the key column to the selection list, but never create a DomainResult
		// as we won't read the value back.  Ideally, we would read the "value column(s)" and
		// update the collection state accordingly much like is done for entity state -
		// however, the concern is minor, so for simplicity we do not.
		querySpec.getSelectClause()
				.addSqlSelection( new SqlSelectionImpl( columnReference, 0 ) );

		final int jdbcTypeCount = keyDescriptor.getJdbcTypeCount();
		final int expectedParamCount = ownerDetailsMap.size() * jdbcTypeCount;
		final var parameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		final InListPredicate restriction;
		if ( jdbcTypeCount == 1 ) {
			restriction = new InListPredicate( columnReference );
			applySimpleCollectionKeyTableLockRestrictions(
					attributeMapping,
					keyDescriptor,
					restriction,
					parameterBindings,
					ownerDetailsMap,
					executionContext.getSession()
			);
		}
		else {
			restriction = applyCompositeCollectionKeyTableLockRestrictions(
					attributeMapping,
					keyDescriptor,
					tableReference,
					parameterBindings,
					ownerDetailsMap,
					executionContext.getSession()
			);
		}
		querySpec.applyPredicate( restriction );

		performLocking( querySpec, parameterBindings,
				lockingExecutionContext( lockMode, lockTimeout, executionContext ) );
	}

	private static ExecutionContext lockingExecutionContext(LockMode lockMode, Timeout lockTimeout, ExecutionContext executionContext) {
		final var lockingQueryOptions = new QueryOptionsImpl();
		final var lockOptions = lockingQueryOptions.getLockOptions();
		lockOptions.setLockMode( lockMode );
		lockOptions.setTimeout( lockTimeout );
		return new BaseExecutionContext( executionContext.getSession() ) {
			@Override
			public QueryOptions getQueryOptions() {
				return lockingQueryOptions;
			}
		};
	}

	private static void applySimpleCollectionKeyTableLockRestrictions(
			PluralAttributeMapping attributeMapping,
			ForeignKeyDescriptor keyDescriptor,
			InListPredicate restriction,
			JdbcParameterBindingsImpl parameterBindings,
			Map<Object, EntityDetails> ownerDetailsMap,
			SharedSessionContractImplementor session) {

		ownerDetailsMap.forEach( (o, entityDetails) -> {
			final var collectionInstance =
					(PersistentCollection<?>)
							entityDetails.entry().getLoadedState()[attributeMapping.getStateArrayPosition()];
			keyDescriptor.breakDownJdbcValues(
					collectionInstance.getKey(),
					(valueIndex, value, jdbcValueMapping) -> {
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						restriction.addExpression( jdbcParameter );
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
		} );
	}

	private static InListPredicate applyCompositeCollectionKeyTableLockRestrictions(
			PluralAttributeMapping attributeMapping,
			ForeignKeyDescriptor keyDescriptor,
			TableReference tableReference,
			JdbcParameterBindingsImpl parameterBindings,
			Map<Object, EntityDetails> ownerDetailsMap,
			SharedSessionContractImplementor session) {
		if ( !session.getDialect().supportsRowValueConstructorSyntaxInInList() ) {
			// for now...
			throw new UnsupportedOperationException(
					"Follow-on collection-table locking with composite keys is not supported for Dialects"
					+ " which do not support tuples (row constructor syntax) as part of an in-list"
			);
		}

		final int jdbcTypeCount = keyDescriptor.getJdbcTypeCount();
		final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
		keyDescriptor.forEachSelectable( (selectionIndex, selectableMapping) -> {
			columnReferences.add( new ColumnReference( tableReference, selectableMapping ) );
		} );
		final InListPredicate inListPredicate = new InListPredicate( new SqlTuple( columnReferences, keyDescriptor ) );

		ownerDetailsMap.forEach( (o, entityDetails) -> {
			final var collectionInstance =
					(PersistentCollection<?>)
							entityDetails.entry().getLoadedState()[attributeMapping.getStateArrayPosition()];
			final Object collectionKeyValue = collectionInstance.getKey();

			final List<JdbcParameter> jdbcParameters = new ArrayList<>( jdbcTypeCount );
			keyDescriptor.breakDownJdbcValues(
					collectionKeyValue,
					(valueIndex, value, jdbcValueMapping) -> {
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						jdbcParameters.add( jdbcParameter );
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
			inListPredicate.addExpression( new SqlTuple( jdbcParameters, keyDescriptor ) );
		} );

		return inListPredicate;
	}

	/**
	 * Lock a collection-table.
	 *
	 * @param attributeMapping The plural attribute whose table needs locked.
	 * @param lockMode The lock mode to apply
	 * @param lockTimeout A lock timeout to apply, if one.
	 * @param collectionKeys Keys of collection-table rows that should be locked.
	 */
	public static void lockCollectionTable(
			PluralAttributeMapping attributeMapping,
			LockMode lockMode,
			Timeout lockTimeout,
			List<CollectionKey> collectionKeys,
			ExecutionContext executionContext) {
		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final String keyTableName = keyDescriptor.getKeyTable();

		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.followOnLockingForCollectionTable( keyTableName, attributeMapping.getRootPathName() );
		}

		final var querySpec = new QuerySpec( true );

		final var tableReference = new NamedTableReference( keyTableName, "tbl" );
		final var tableGroup = new LockingTableGroup(
				tableReference,
				keyTableName,
				attributeMapping,
				keyDescriptor.getKeySide().getModelPart()
		);

		querySpec.getFromClause().addRoot( tableGroup );

		final var keyPart = keyDescriptor.getKeyPart();
		final var columnReference = new ColumnReference( tableReference, keyPart.getSelectable( 0 ) );

		// NOTE: We add the key column to the selection list, but never create a DomainResult
		// as we won't read the value back.  Ideally, we would read the "value column(s)" and
		// update the collection state accordingly much like is done for entity state -
		// however, the concern is minor, so for simplicity we do not.
		final var sqlSelection = new SqlSelectionImpl( columnReference, 0 );
		querySpec.getSelectClause().addSqlSelection( sqlSelection );

		final int jdbcTypeCount = keyDescriptor.getJdbcTypeCount();
		final int expectedParamCount = collectionKeys.size() * jdbcTypeCount;
		final var parameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		final InListPredicate restriction;
		if ( jdbcTypeCount == 1 ) {
			restriction = new InListPredicate( columnReference );
			applySimpleCollectionKeyTableLockRestrictions(
					keyDescriptor,
					restriction,
					parameterBindings,
					collectionKeys,
					executionContext.getSession()
			);
		}
		else {
			restriction = applyCompositeCollectionKeyTableLockRestrictions(
					attributeMapping,
					keyDescriptor,
					tableReference,
					parameterBindings,
					collectionKeys,
					executionContext.getSession()
			);
		}
		querySpec.applyPredicate( restriction );

		performLocking( querySpec, parameterBindings,
				lockingExecutionContext( lockMode, lockTimeout, executionContext ) );
	}

	private static void applySimpleCollectionKeyTableLockRestrictions(
			ForeignKeyDescriptor keyDescriptor,
			InListPredicate restriction,
			JdbcParameterBindingsImpl parameterBindings,
			List<CollectionKey> collectionKeys,
			SharedSessionContractImplementor session) {
		for ( var collectionKey : collectionKeys ) {
			keyDescriptor.breakDownJdbcValues(
					collectionKey.getKey(),
					(valueIndex, value, jdbcValueMapping) -> {
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						restriction.addExpression( jdbcParameter );
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
		}
	}

	private static InListPredicate applyCompositeCollectionKeyTableLockRestrictions(
			PluralAttributeMapping attributeMapping,
			ForeignKeyDescriptor keyDescriptor,
			TableReference tableReference,
			JdbcParameterBindingsImpl parameterBindings,
			List<CollectionKey> collectionKeys,
			SharedSessionContractImplementor session) {
		if ( !session.getDialect().supportsRowValueConstructorSyntaxInInList() ) {
			// for now...
			throw new UnsupportedOperationException(
					"Follow-on collection-table locking with composite keys is not supported for Dialects"
					+ " which do not support tuples (row constructor syntax) as part of an in-list"
			);
		}

		final int jdbcTypeCount = keyDescriptor.getJdbcTypeCount();
		final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
		keyDescriptor.forEachSelectable( (selectionIndex, selectableMapping) -> {
			columnReferences.add( new ColumnReference( tableReference, selectableMapping ) );
		} );
		final var inListPredicate = new InListPredicate( new SqlTuple( columnReferences, keyDescriptor ) );

		for ( var collectionKey : collectionKeys ) {
			final List<JdbcParameter> jdbcParameters = new ArrayList<>( jdbcTypeCount );
			keyDescriptor.breakDownJdbcValues(
					collectionKey.getKey(),
					(valueIndex, value, jdbcValueMapping) -> {
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						jdbcParameters.add( jdbcParameter );
						parameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
			inListPredicate.addExpression( new SqlTuple( jdbcParameters, keyDescriptor ) );
		}

		return inListPredicate;
	}

	private static void performLocking(
			QuerySpec querySpec,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext lockingExecutionContext) {
		final var sessionFactory = lockingExecutionContext.getSession().getSessionFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		jdbcServices.getJdbcSelectExecutor().executeQuery(
				jdbcServices.getDialect().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, new SelectStatement( querySpec ) )
						.translate( jdbcParameterBindings, lockingExecutionContext.getQueryOptions() ),
				jdbcParameterBindings,
				lockingExecutionContext,
				row -> row,
				Object[].class,
				StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
				ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.ALLOW )
		);
	}

	/**
	 * Log information about the entries in LoadedValuesCollector.
	 */
	public static void logLoadedValues(LoadedValuesCollector collector) {
		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			var summary = new StringBuilder();
			summary.append( "  Loaded root entities:\n" );
			collector.getCollectedRootEntities().forEach( (reg) -> {
				summary.append( String.format( "    - %s#%s\n",
						reg.entityDescriptor().getEntityName(),
						reg.entityKey().getIdentifier() ) );
			} );

			summary.append( "  Loaded non-root entities:\n" );
			collector.getCollectedNonRootEntities().forEach( (reg) -> {
				summary.append( String.format( "    - %s#%s\n",
						reg.entityDescriptor().getEntityName()
						, reg.entityKey().getIdentifier() ) );
			} );

			summary.append( "  Loaded collections:\n" );
			collector.getCollectedCollections().forEach( (reg) -> {
				summary.append( String.format( "    - %s#%s\n",
						reg.collectionDescriptor().getRootPathName(),
						reg.collectionKey().getKey() ) );
			} );
			SQL_EXEC_LOGGER.followOnLockingCollectedLoadedValues( summary.toString() );
		}
	}

	/**
	 * Extracts all NavigablePaths to lock based on the {@linkplain LockingClauseStrategy locking strategy}
	 * from the {@linkplain SqlAstTranslator SQL AST translator}.
	 */
	public static Collection<NavigablePath> extractPathsToLock(LockingClauseStrategy lockingClauseStrategy) {
		final LinkedHashSet<NavigablePath> paths = new LinkedHashSet<>();

		final var rootsToLock = lockingClauseStrategy.getRootsToLock();
		if ( rootsToLock != null ) {
			rootsToLock.forEach( (tableGroup) -> paths.add( tableGroup.getNavigablePath() ) );
		}

		final var joinsToLock = lockingClauseStrategy.getJoinsToLock();
		if ( joinsToLock != null ) {
			joinsToLock.forEach( (tableGroupJoin) -> {
				final var navigablePath = tableGroupJoin.getNavigablePath();
				paths.add( navigablePath );
				final var modelPart = tableGroupJoin.getJoinedGroup().getModelPart();
				if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
					paths.add( navigablePath.append( pluralAttributeMapping.getElementDescriptor().getPartName() ) );
					final var indexDescriptor = pluralAttributeMapping.getIndexDescriptor();
					if ( indexDescriptor != null ) {
						paths.add( navigablePath.append( indexDescriptor.getPartName() ) );
					}
				}
			} );
		}
		return paths;
	}

	public static void segmentLoadedValues(List<LoadedValuesCollector.LoadedEntityRegistration> registrations, Map<EntityMappingType, List<EntityKey>> map) {
		if ( registrations != null ) {
			registrations.forEach( (registration) -> {
				final var entityKeys =
						map.computeIfAbsent( registration.entityDescriptor(),
								entityMappingType -> new ArrayList<>() );
				entityKeys.add( registration.entityKey() );
			} );
		}
	}

	public static void segmentLoadedCollections(List<LoadedValuesCollector.LoadedCollectionRegistration> registrations, Map<EntityMappingType, Map<PluralAttributeMapping, List<CollectionKey>>> map) {
		if ( registrations != null ) {
			registrations.forEach( (registration) -> {
				final var pluralAttributeMapping = registration.collectionDescriptor();
				if ( pluralAttributeMapping.getSeparateCollectionTable() != null ) {
					final var attributeKeys =
							map.computeIfAbsent( pluralAttributeMapping.findContainingEntityMapping(),
									entityMappingType -> new HashMap<>() );
					final var collectionKeys =
							attributeKeys.computeIfAbsent( pluralAttributeMapping,
									entityMappingType -> new ArrayList<>() );
					collectionKeys.add( registration.collectionKey() );
				}
			} );
		}
	}

	public static Map<Object, EntityDetails> resolveEntityKeys(List<EntityKey> entityKeys, ExecutionContext executionContext) {
		final Map<Object, EntityDetails> map = new HashMap<>();
		final var persistenceContext = executionContext.getSession().getPersistenceContext();
		entityKeys.forEach( (entityKey) -> {
			final Object instance = persistenceContext.getEntity( entityKey );
			final var entityEntry = persistenceContext.getEntry( instance );
			map.put( entityKey.getIdentifierValue(), new EntityDetails( entityKey, entityEntry, instance ) );
		} );
		return map;
	}
}
