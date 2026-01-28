/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableHelper;
import org.hibernate.dialect.temptable.TemporaryTableHelper.TemporaryTableCreationWork;
import org.hibernate.dialect.temptable.TemporaryTableHelper.TemporaryTableDropWork;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.hibernate.boot.TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT;
import static org.hibernate.boot.TempTableDdlTransactionHandling.NONE;

/**
 * @author Steve Ebersole
 */
public final class ExecuteWithTemporaryTableHelper {
	private ExecuteWithTemporaryTableHelper() {
	}

	public static CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> createMatchingIdsIntoIdTableInsert(
			MultiTableSqmMutationConverter sqmConverter,
			Predicate suppliedPredicate,
			TemporaryTable idTable,
			JdbcParameter sessionUidParameter,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final var mutatingTableGroup = sqmConverter.getMutatingTableGroup();
		final var mutatingEntityDescriptor = (EntityMappingType) mutatingTableGroup.getModelPart();

		final var idTableReference = new NamedTableReference(
				idTable.getTableExpression(),
				InsertSelectStatement.DEFAULT_ALIAS
		);
		final var idTableInsert = new InsertSelectStatement( idTableReference,
				mutatingEntityDescriptor.getEntityPersister() );

		for ( int i = 0; i < idTable.getColumns().size(); i++ ) {
			final var temporaryTableColumn = idTable.getColumns().get( i );
			idTableInsert.addTargetColumnReferences(
					new ColumnReference(
							idTableReference,
							temporaryTableColumn.getColumnName(),
							// id columns cannot be formulas and cannot have custom read and write expressions
							false,
							null,
							temporaryTableColumn.getJdbcMapping()
					)
			);
		}

		final var matchingIdSelection = new QuerySpec( true, 1 );
		idTableInsert.setSourceSelectStatement( matchingIdSelection );

		matchingIdSelection.getFromClause().addRoot( mutatingTableGroup );

		mutatingEntityDescriptor.getIdentifierMapping().forEachSelectable(
				(selectionIndex, selection) -> {
					matchingIdSelection.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									selectionIndex,
									sqmConverter.getSqlExpressionResolver().resolveSqlExpression(
											mutatingTableGroup.resolveTableReference(
													mutatingTableGroup.getNavigablePath(),
													selection.getContainingTableExpression()
											),
											selection
									)
							)
					);
				}
		);

		if ( idTable.getSessionUidColumn() != null ) {
			final int jdbcPosition = matchingIdSelection.getSelectClause().getSqlSelections().size();
			matchingIdSelection.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( jdbcPosition, sessionUidParameter )
			);
		}

		matchingIdSelection.applyPredicate( suppliedPredicate );

		return createTemporaryTableInsert( idTableInsert, jdbcParameterBindings, executionContext );
	}

	public static CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> createTemporaryTableInsert(
			InsertSelectStatement temporaryTableInsert,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		final var jdbcEnvironment = factory.getJdbcServices().getJdbcEnvironment();
		final var lockOptions = executionContext.getQueryOptions().getLockOptions();
		final var lockMode = lockOptions.getLockMode();
		// Acquire a WRITE lock for the rows that are about to be modified
		lockOptions.setLockMode( LockMode.WRITE );
		// Visit the table joins and reset the lock mode if we encounter OUTER joins that are not supported
		final var sourceSelectStatement = temporaryTableInsert.getSourceSelectStatement();
		if ( sourceSelectStatement != null
			&& !jdbcEnvironment.getDialect().supportsOuterJoinForUpdate() ) {
			sourceSelectStatement.visitQuerySpecs( querySpec -> {
				querySpec.getFromClause().visitTableJoins( tableJoin -> {
					if ( tableJoin.isInitialized()
						&& tableJoin.getJoinType() != SqlAstJoinType.INNER ) {
						lockOptions.setLockMode( lockMode );
					}
				} );
			} );
		}
		lockOptions.setLockMode( lockMode );
		return new CacheableSqmInterpretation<>(
				temporaryTableInsert,
				jdbcEnvironment.getSqlAstTranslatorFactory()
						.buildMutationTranslator( factory, temporaryTableInsert )
						.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
				Map.of(),
				Map.of()
		);
	}

	@Deprecated(forRemoval = true, since = "7.3") // no longer used
	public static int saveIntoTemporaryTable(
			InsertSelectStatement temporaryTableInsert,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		final var jdbcEnvironment = factory.getJdbcServices().getJdbcEnvironment();
		final var lockOptions = executionContext.getQueryOptions().getLockOptions();
		final var lockMode = lockOptions.getLockMode();
		// Acquire a WRITE lock for the rows that are about to be modified
		lockOptions.setLockMode( LockMode.WRITE );
		// Visit the table joins and reset the lock mode if we encounter OUTER joins that are not supported
		final var sourceSelectStatement = temporaryTableInsert.getSourceSelectStatement();
		if ( sourceSelectStatement != null
			&& !jdbcEnvironment.getDialect().supportsOuterJoinForUpdate() ) {
			sourceSelectStatement.visitQuerySpecs( querySpec -> {
				querySpec.getFromClause().visitTableJoins( tableJoin -> {
					if ( tableJoin.isInitialized()
						&& tableJoin.getJoinType() != SqlAstJoinType.INNER ) {
						lockOptions.setLockMode( lockMode );
					}
				} );
			} );
		}
		lockOptions.setLockMode( lockMode );
		return saveIntoTemporaryTable(
				jdbcEnvironment.getSqlAstTranslatorFactory()
						.buildMutationTranslator( factory, temporaryTableInsert )
						.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
				jdbcParameterBindings,
				executionContext
		);
	}

	public static int saveIntoTemporaryTable(
			JdbcOperationQueryMutation jdbcInsert,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		return executionContext.getSession().getFactory().getJdbcServices().getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession().getJdbcCoordinator()
						.getStatementPreparer().prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	public static QuerySpec createIdTableSelectQuerySpec(
			TemporaryTable idTable,
			JdbcParameter sessionUidParameter,
			EntityMappingType entityDescriptor,
			ExecutionContext executionContext) {
		return createIdTableSelectQuerySpec(
				idTable,
				null,
				sessionUidParameter,
				entityDescriptor,
				executionContext
		);
	}

	public static QuerySpec createIdTableSelectQuerySpec(
			TemporaryTable idTable,
			ModelPart fkModelPart,
			JdbcParameter sessionUidParameter,
			EntityMappingType entityDescriptor,
			ExecutionContext executionContext) {
		final var querySpec = new QuerySpec( false );

		final var idTableReference = new NamedTableReference(
				idTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true
		);
		final var idTableGroup = new StandardTableGroup(
				true,
				new NavigablePath( idTableReference.getTableExpression() ),
				entityDescriptor,
				null,
				idTableReference,
				null,
				executionContext.getSession().getFactory()
		);

		querySpec.getFromClause().addRoot( idTableGroup );

		applyIdTableSelections( querySpec, idTableReference, idTable, fkModelPart, entityDescriptor );
		applyIdTableRestrictions( querySpec, idTableReference, idTable, sessionUidParameter );

		return querySpec;
	}

	private static void applyIdTableSelections(
			QuerySpec querySpec,
			TableReference tableReference,
			TemporaryTable idTable,
			ModelPart fkModelPart,
			EntityMappingType entityDescriptor) {
		if ( fkModelPart == null ) {
			final int size = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
			for ( int i = 0; i < size; i++ ) {
				final var temporaryTableColumn = idTable.getColumns().get( i );
				if ( temporaryTableColumn != idTable.getSessionUidColumn() ) {
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									i,
									new ColumnReference(
											tableReference,
											temporaryTableColumn.getColumnName(),
											false,
											null,
											temporaryTableColumn.getJdbcMapping()
									)
							)
					);
				}
			}
		}
		else {
			fkModelPart.forEachSelectable( (i, selectableMapping) -> {
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								i,
								new ColumnReference(
										tableReference,
										selectableMapping.getSelectionExpression(),
										false,
										null,
										selectableMapping.getJdbcMapping()
								)
						)
				);
			} );
		}
	}

	private static void applyIdTableRestrictions(
			QuerySpec querySpec,
			TableReference idTableReference,
			TemporaryTable idTable,
			JdbcParameter sessionUidParameter) {
		final var sessionUidColumn = idTable.getSessionUidColumn();
		if ( sessionUidColumn != null ) {
			querySpec.applyPredicate(
					new ComparisonPredicate(
							new ColumnReference(
									idTableReference,
									sessionUidColumn.getColumnName(),
									false,
									null,
									sessionUidColumn.getJdbcMapping()
							),
							ComparisonOperator.EQUAL,
							sessionUidParameter
					)
			);
		}
	}

	@Deprecated(forRemoval = true, since = "7.1")
	public static void performBeforeTemporaryTableUseActions(
			TemporaryTable temporaryTable,
			ExecutionContext executionContext) {
		performBeforeTemporaryTableUseActions(
				temporaryTable,
				executionContext.getSession().getDialect()
						.getTemporaryTableBeforeUseAction(),
				executionContext
		);
	}

	public static boolean performBeforeTemporaryTableUseActions(
			TemporaryTable temporaryTable,
			TemporaryTableStrategy temporaryTableStrategy,
			ExecutionContext executionContext) {
		return performBeforeTemporaryTableUseActions(
				temporaryTable,
				temporaryTableStrategy.getTemporaryTableBeforeUseAction(),
				executionContext
		);
	}

	private static boolean performBeforeTemporaryTableUseActions(
			TemporaryTable temporaryTable,
			BeforeUseAction beforeUseAction,
			ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		final var dialect = factory.getJdbcServices().getDialect();
		return beforeUseAction == BeforeUseAction.CREATE
			&& doWork( executionContext, dialect,
				new TemporaryTableCreationWork( temporaryTable, factory ) );
	}

//	public static int[] loadInsertedRowNumbers(
//			TemporaryTable temporaryTable,
//			Function<SharedSessionContractImplementor, String> sessionUidAccess,
//			int rows,
//			ExecutionContext executionContext) {
//		final String sqlSelect =
//				createInsertedRowNumbersSelectSql( temporaryTable, sessionUidAccess, executionContext );
//		return loadInsertedRowNumbers( sqlSelect, temporaryTable, sessionUidAccess, rows, executionContext );
//	}

	public static int[] loadInsertedRowNumbers(
			String sqlSelect,
			TemporaryTable temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			int rows,
			ExecutionContext executionContext) {
		final var sessionUidColumn = temporaryTable.getSessionUidColumn();
		final var session = executionContext.getSession();
		final var jdbcCoordinator = session.getJdbcCoordinator();
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( sqlSelect );
			if ( sessionUidColumn != null ) {
				//noinspection unchecked
				sessionUidColumn.getJdbcMapping().getJdbcValueBinder().bind(
						preparedStatement,
						UUID.fromString( sessionUidAccess.apply( session ) ),
						1,
						session
				);
			}
			final var resultSet = jdbcCoordinator.getResultSetReturn().execute( preparedStatement, sqlSelect );
			final var rowNumbers = new int[rows];
			try {
				int rowIndex = 0;
				while (resultSet.next()) {
					rowNumbers[rowIndex++] = resultSet.getInt( 1 );
				}
				return rowNumbers;
			}
			catch ( IndexOutOfBoundsException e ) {
				throw new IllegalArgumentException( "Expected " + rows + " to be inserted but found more", e );
			}
		}
		catch( SQLException ex ) {
			throw new IllegalStateException( ex );
		}
		finally {
			if ( preparedStatement != null ) {
				try {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
				}
				catch( Throwable ignore ) {
					// ignore
				}
				jdbcCoordinator.afterStatementExecution();
			}
		}
	}

	public static String createInsertedRowNumbersSelectSql(
			TemporaryTable temporaryTable,
			ExecutionContext executionContext) {
		final var sessionUidColumn = temporaryTable.getSessionUidColumn();

		final var rowNumberColumn =
				temporaryTable.getColumns()
						.get( temporaryTable.getColumns().size() - (sessionUidColumn == null ? 1 : 2 ) );
		assert rowNumberColumn != null;

		final var session = executionContext.getSession();
		final var simpleSelect =
				new SimpleSelect( session.getFactory() )
						.setTableName( temporaryTable.getQualifiedTableName() )
						.addColumn( rowNumberColumn.getColumnName() );
		if ( sessionUidColumn != null ) {
			simpleSelect.addRestriction( sessionUidColumn.getColumnName() );
		}
		return simpleSelect.toStatementString();
	}

	public static void performAfterTemporaryTableUseActions(
			TemporaryTable temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			AfterUseAction afterUseAction,
			ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		final var dialect = factory.getJdbcServices().getDialect();
		switch ( afterUseAction ) {
			case CLEAN:
				TemporaryTableHelper.cleanTemporaryTableRows(
						temporaryTable,
						dialect.getTemporaryTableExporter(),
						sessionUidAccess,
						executionContext.getSession()
				);
				break;
			case DROP:
				doWork( executionContext, dialect,
						new TemporaryTableDropWork( temporaryTable, factory ) );
		}
	}

	private static <T> T doWork(
			ExecutionContext executionContext, Dialect dialect,
			AbstractReturningWork<T> temporaryTableCreationWork) {
		final var ddlTransactionHandling = dialect.getTemporaryTableDdlTransactionHandling();
		if ( ddlTransactionHandling == NONE ) {
			return executionContext.getSession().doReturningWork( temporaryTableCreationWork );
		}
		else {
			// this branch is obsolete, since
			// dialect.getTemporaryTableDdlTransactionHandling()
			// now always returns NONE
			return executionContext.getSession().getJdbcCoordinator().getJdbcSessionOwner()
					.getTransactionCoordinator().createIsolationDelegate()
					.delegateWork( temporaryTableCreationWork,
							ddlTransactionHandling == ISOLATE_AND_TRANSACT );
		}
	}
	private static void doWork(
			ExecutionContext executionContext, Dialect dialect,
			AbstractWork temporaryTableDropWork) {
		final var ddlTransactionHandling = dialect.getTemporaryTableDdlTransactionHandling();
		if ( ddlTransactionHandling == NONE ) {
			executionContext.getSession().doWork( temporaryTableDropWork );
		}
		else {
			// this branch is obsolete, since
			// dialect.getTemporaryTableDdlTransactionHandling()
			// now always returns NONE
			executionContext.getSession().getJdbcCoordinator().getJdbcSessionOwner()
					.getTransactionCoordinator().createIsolationDelegate()
					.delegateWork( temporaryTableDropWork,
							ddlTransactionHandling == ISOLATE_AND_TRANSACT );
		}
	}
}
