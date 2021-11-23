/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public final class ExecuteWithIdTableHelper {
	private ExecuteWithIdTableHelper() {
	}

	public static int saveMatchingIdsIntoIdTable(
			MultiTableSqmMutationConverter sqmConverter,
			Predicate suppliedPredicate,
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();

		assert mutatingTableGroup.getModelPart() instanceof EntityMappingType;
		final EntityMappingType mutatingEntityDescriptor = (EntityMappingType) mutatingTableGroup.getModelPart();

		final TableReference idTableReference = new TableReference( idTable.getTableExpression(), null, false, factory );
		final InsertStatement idTableInsert = new InsertStatement( idTableReference );

		for ( int i = 0; i < idTable.getIdTableColumns().size(); i++ ) {
			final IdTableColumn column = idTable.getIdTableColumns().get( i );
			idTableInsert.addTargetColumnReferences(
					new ColumnReference(
							idTableReference,
							column.getColumnName(),
							// id columns cannot be formulas and cannot have custom read and write expressions
							false,
							null,
							null,
							column.getJdbcMapping(),
							factory
					)
			);
		}

		final QuerySpec matchingIdSelection = new QuerySpec( true, 1 );
		idTableInsert.setSourceSelectStatement( matchingIdSelection );

		matchingIdSelection.getFromClause().addRoot( mutatingTableGroup );

		mutatingEntityDescriptor.getIdentifierMapping().forEachSelectable(
				(jdbcPosition, selection) -> {
					final TableReference tableReference = mutatingTableGroup.resolveTableReference(
							mutatingTableGroup.getNavigablePath(),
							selection.getContainingTableExpression()
					);
					matchingIdSelection.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									jdbcPosition,
									jdbcPosition + 1,
									sqmConverter.getSqlExpressionResolver().resolveSqlExpression(
											SqlExpressionResolver.createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
											sqlAstProcessingState -> new ColumnReference(
													tableReference,
													selection,
													factory
											)
									)
							)
					);
				}
		);

		if ( idTable.getSessionUidColumn() != null ) {
			final int jdbcPosition = matchingIdSelection.getSelectClause().getSqlSelections().size();
			matchingIdSelection.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							jdbcPosition,
							jdbcPosition + 1,
							new QueryLiteral<>(
									sessionUidAccess.apply( executionContext.getSession() ),
									(BasicValuedMapping) idTable.getSessionUidColumn().getJdbcMapping()
							)
					)
			);
		}

		matchingIdSelection.applyPredicate( suppliedPredicate );

		final JdbcServices jdbcServices = factory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final LockOptions lockOptions = executionContext.getQueryOptions().getLockOptions();
		final LockMode lockMode = lockOptions.getLockMode();
		// Acquire a WRITE lock for the rows that are about to be modified
		lockOptions.setLockMode( LockMode.WRITE );
		// Visit the table joins and reset the lock mode if we encounter OUTER joins that are not supported
		if ( !jdbcEnvironment.getDialect().supportsOuterJoinForUpdate() ) {
			matchingIdSelection.getFromClause().visitTableJoins(
					tableJoin -> {
						if ( tableJoin.getJoinType() != SqlAstJoinType.INNER ) {
							lockOptions.setLockMode( lockMode );
						}
					}
			);
		}
		final JdbcInsert jdbcInsert = sqlAstTranslatorFactory.buildInsertTranslator( factory, idTableInsert )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
		lockOptions.setLockMode( lockMode );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	public static QuerySpec createIdTableSelectQuerySpec(
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			EntityMappingType entityDescriptor,
			ExecutionContext executionContext) {
		return createIdTableSelectQuerySpec( idTable, null, sessionUidAccess, entityDescriptor, executionContext );
	}

	public static QuerySpec createIdTableSelectQuerySpec(
			IdTable idTable,
			ModelPart fkModelPart,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			EntityMappingType entityDescriptor,
			ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference idTableReference = new TableReference(
				idTable.getTableExpression(),
				IdTable.DEFAULT_ALIAS,
				true,
				executionContext.getSession().getFactory()
		);
		final TableGroup idTableGroup = new StandardTableGroup(
				true,
				new NavigablePath( idTableReference.getTableExpression() ),
				entityDescriptor,
				null,
				idTableReference,
				null,
				executionContext.getSession().getFactory()
		);

		querySpec.getFromClause().addRoot( idTableGroup );

		applyIdTableSelections( querySpec, idTableReference, idTable, fkModelPart, executionContext );
		applyIdTableRestrictions( querySpec, idTableReference, idTable, sessionUidAccess, executionContext );

		return querySpec;
	}

	private static void applyIdTableSelections(
			QuerySpec querySpec,
			TableReference tableReference,
			IdTable idTable,
			ModelPart fkModelPart,
			ExecutionContext executionContext) {
		if ( fkModelPart == null ) {
			final int size = idTable.getEntityDescriptor().getIdentifierMapping().getJdbcTypeCount();
			for ( int i = 0; i < size; i++ ) {
				final IdTableColumn idTableColumn = idTable.getIdTableColumns().get( i );
				if ( idTableColumn != idTable.getSessionUidColumn() ) {
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									i + 1,
									i,
									new ColumnReference(
											tableReference,
											idTableColumn.getColumnName(),
											false,
											null,
											null,
											idTableColumn.getJdbcMapping(),
											executionContext.getSession().getFactory()
									)
							)
					);
				}
			}
		}
		else {
			fkModelPart.forEachSelectable(
					(i, selectableMapping) -> {
						querySpec.getSelectClause().addSqlSelection(
								new SqlSelectionImpl(
										i + 1,
										i,
										new ColumnReference(
												tableReference,
												selectableMapping.getSelectionExpression(),
												false,
												null,
												null,
												selectableMapping.getJdbcMapping(),
												executionContext.getSession().getFactory()
										)
								)
						);
					}
			);
		}
	}

	private static void applyIdTableRestrictions(
			QuerySpec querySpec,
			TableReference idTableReference,
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			ExecutionContext executionContext) {
		if ( idTable.getSessionUidColumn() != null ) {
			querySpec.applyPredicate(
					new ComparisonPredicate(
							new ColumnReference(
									idTableReference,
									idTable.getSessionUidColumn().getColumnName(),
									false,
									null,
									null,
									idTable.getSessionUidColumn().getJdbcMapping(),
									executionContext.getSession().getFactory()
							),
							ComparisonOperator.EQUAL,
							new QueryLiteral<>(
									sessionUidAccess.apply( executionContext.getSession() ),
									(BasicValuedMapping) idTable.getSessionUidColumn().getJdbcMapping()
							)
					)
			);
		}
	}

	public static void performBeforeIdTableUseActions(
			BeforeUseAction beforeUseAction,
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			ExecutionContext executionContext) {
		if ( beforeUseAction == BeforeUseAction.CREATE ) {
			final IdTableHelper.IdTableCreationWork idTableCreationWork = new IdTableHelper.IdTableCreationWork(
					idTable,
					idTableExporterAccess.get(),
					executionContext.getSession().getFactory()
			);

			if ( ddlTransactionHandling == TempTableDdlTransactionHandling.NONE ) {
				executionContext.getSession().doWork( idTableCreationWork );
			}
			else {
				final IsolationDelegate isolationDelegate = executionContext.getSession()
						.getJdbcCoordinator()
						.getJdbcSessionOwner()
						.getTransactionCoordinator()
						.createIsolationDelegate();
				isolationDelegate.delegateWork( idTableCreationWork, ddlTransactionHandling == TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT );
			}
		}
	}

	public static void performAfterIdTableUseActions(
			AfterUseAction afterUseAction,
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			ExecutionContext executionContext) {
		if ( afterUseAction == AfterUseAction.CLEAN ) {
			IdTableHelper.cleanIdTableRows(
					idTable,
					idTableExporterAccess.get(),
					sessionUidAccess,
					executionContext.getSession()
			);
		}
		else if ( afterUseAction == AfterUseAction.DROP ) {
			final IdTableHelper.IdTableDropWork idTableDropWork = new IdTableHelper.IdTableDropWork(
					idTable,
					idTableExporterAccess.get(),
					executionContext.getSession().getFactory()
			);

			if ( ddlTransactionHandling == TempTableDdlTransactionHandling.NONE ) {
				executionContext.getSession().doWork( idTableDropWork );
			}
			else {
				final IsolationDelegate isolationDelegate = executionContext.getSession()
						.getJdbcCoordinator()
						.getJdbcSessionOwner()
						.getTransactionCoordinator()
						.createIsolationDelegate();
				isolationDelegate.delegateWork( idTableDropWork, ddlTransactionHandling == TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT );
			}
		}
	}
}
