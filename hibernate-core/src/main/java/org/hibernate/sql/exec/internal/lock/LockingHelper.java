/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;

/**
 * Helper for dealing with follow-on locking for collection-tables.
 *
 * @author Steve Ebersole
 */
public class LockingHelper {
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
		final ForeignKeyDescriptor keyDescriptor = attributeMapping.getKeyDescriptor();
		final String keyTableName = keyDescriptor.getKeyTable();

		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.debugf( "Follow-on locking for collection table `%s` - %s", keyTableName, attributeMapping.getRootPathName() );
		}

		final QuerySpec querySpec = new QuerySpec( true );

		final NamedTableReference tableReference = new NamedTableReference( keyTableName, "tbl" );
		final LockingTableGroup tableGroup = new LockingTableGroup(
				tableReference,
				keyTableName,
				attributeMapping,
				keyDescriptor.getKeySide().getModelPart()
		);

		querySpec.getFromClause().addRoot( tableGroup );

		final ValuedModelPart keyPart = keyDescriptor.getKeyPart();
		final ColumnReference columnReference = new ColumnReference( tableReference, keyPart.getSelectable( 0 ) );

		// NOTE: We add the key column to the selection list, but never create a DomainResult
		// as we won't read the value back.  Ideally, we would read the "value column(s)" and
		// update the collection state accordingly much like is done for entity state -
		// however, the concern is minor, so for simplicity we do not.
		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( columnReference, 0 );
		querySpec.getSelectClause().addSqlSelection( sqlSelection );

		final InListPredicate restriction = new InListPredicate( columnReference );
		querySpec.applyPredicate( restriction );

		final int expectedParamCount = ownerDetailsMap.size() * keyDescriptor.getJdbcTypeCount();
		final JdbcParameterBindingsImpl parameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		if ( keyDescriptor.getJdbcTypeCount() == 1 ) {
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
			applyCompositeCollectionKeyTableLockRestrictions(
					attributeMapping,
					keyDescriptor,
					restriction,
					parameterBindings,
					ownerDetailsMap,
					executionContext.getSession()
			);
		}

		final QueryOptionsImpl lockingQueryOptions = new QueryOptionsImpl();
		lockingQueryOptions.getLockOptions().setLockMode( lockMode );
		lockingQueryOptions.getLockOptions().setTimeout( lockTimeout );
		final ExecutionContext lockingExecutionContext = new BaseExecutionContext( executionContext.getSession() ) {
			@Override
			public QueryOptions getQueryOptions() {
				return lockingQueryOptions;
			}
		};

		performLocking( querySpec, parameterBindings, lockingExecutionContext );
	}

	private static void applySimpleCollectionKeyTableLockRestrictions(
			PluralAttributeMapping attributeMapping,
			ForeignKeyDescriptor keyDescriptor,
			InListPredicate restriction,
			JdbcParameterBindingsImpl parameterBindings,
			Map<Object, EntityDetails> ownerDetailsMap,
			SharedSessionContractImplementor session) {

		ownerDetailsMap.forEach( (o, entityDetails) -> {
			final PersistentCollection<?> collectionInstance = (PersistentCollection<?>) entityDetails.entry().getLoadedState()[attributeMapping.getStateArrayPosition()];
			final Object collectionKeyValue = collectionInstance.getKey();
			keyDescriptor.breakDownJdbcValues(
					collectionKeyValue,
					(valueIndex, value, jdbcValueMapping) -> {
						final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl(
								jdbcValueMapping.getJdbcMapping() );
						restriction.addExpression( jdbcParameter );

						parameterBindings.addBinding(
								jdbcParameter,
								new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), value )
						);
					},
					session
			);
		} );
	}

	private static void applyCompositeCollectionKeyTableLockRestrictions(
			PluralAttributeMapping attributeMapping,
			ForeignKeyDescriptor keyDescriptor,
			InListPredicate restriction,
			JdbcParameterBindingsImpl parameterBindings,
			Map<Object, EntityDetails> ownerDetailsMap,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	private static void performLocking(
			QuerySpec querySpec,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext lockingExecutionContext) {
		final SessionFactoryImplementor sessionFactory = lockingExecutionContext.getSession().getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

		final SelectStatement selectStatement = new SelectStatement( querySpec );
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcServices.getDialect().getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, selectStatement );
		final JdbcOperationQuerySelect jdbcOperation = translator.translate( jdbcParameterBindings, lockingExecutionContext.getQueryOptions() );

		final JdbcSelectExecutor jdbcSelectExecutor = jdbcServices.getJdbcSelectExecutor();
		jdbcSelectExecutor.executeQuery(
				jdbcOperation,
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

	/**
	 * Extracts all NavigablePaths to lock based on the {@linkplain LockingClauseStrategy locking strategy}
	 * from the {@linkplain SqlAstTranslator SQL AST translator}.
	 */
	public static Collection<NavigablePath> extractPathsToLock(LockingClauseStrategy lockingClauseStrategy) {
		final LinkedHashSet<NavigablePath> paths = new LinkedHashSet<>();

		final Collection<TableGroup> rootsToLock = lockingClauseStrategy.getRootsToLock();
		if ( rootsToLock != null ) {
			rootsToLock.forEach( (tableGroup) -> paths.add( tableGroup.getNavigablePath() ) );
		}

		final Collection<TableGroupJoin> joinsToLock = lockingClauseStrategy.getJoinsToLock();
		if ( joinsToLock != null ) {
			joinsToLock.forEach( (tableGroupJoin) -> {
				paths.add( tableGroupJoin.getNavigablePath() );

				final ModelPartContainer modelPart = tableGroupJoin.getJoinedGroup().getModelPart();
				if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
					final NavigablePath elementPath = tableGroupJoin.getNavigablePath().append( pluralAttributeMapping.getElementDescriptor().getPartName() );
					paths.add( elementPath );

					if ( pluralAttributeMapping.getIndexDescriptor() != null ) {
						final NavigablePath indexPath = tableGroupJoin.getNavigablePath().append( pluralAttributeMapping.getIndexDescriptor().getPartName() );
						paths.add( indexPath );
					}
				}
			} );
		}
		return paths;
	}
}
