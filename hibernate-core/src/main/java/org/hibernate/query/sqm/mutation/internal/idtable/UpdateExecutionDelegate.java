/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstUpdateTranslator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class UpdateExecutionDelegate implements TableBasedUpdateHandler.ExecutionDelegate {
	private final SqmUpdateStatement sqmUpdate;
	private final MultiTableSqmMutationConverter sqmConverter;
	private final IdTable idTable;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final Supplier<IdTableExporter> idTableExporterAccess;
	private final DomainParameterXref domainParameterXref;
	private final TableGroup updatingTableGroup;
	private final Predicate suppliedPredicate;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;

	private final Map<TableReference, List<Assignment>> assignmentsByTable;
	private final SessionFactoryImplementor sessionFactory;

	public UpdateExecutionDelegate(
			SqmUpdateStatement sqmUpdate,
			MultiTableSqmMutationConverter sqmConverter,
			IdTable idTable,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			Supplier<IdTableExporter> idTableExporterAccess,
			DomainParameterXref domainParameterXref,
			TableGroup updatingTableGroup,
			TableReference hierarchyRootTableReference,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			Predicate suppliedPredicate,
			Map<SqmParameter, List<JdbcParameter>> parameterResolutions,
			ExecutionContext executionContext) {
		this.sqmUpdate = sqmUpdate;
		this.sqmConverter = sqmConverter;
		this.idTable = idTable;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.idTableExporterAccess = idTableExporterAccess;
		this.domainParameterXref = domainParameterXref;
		this.updatingTableGroup = updatingTableGroup;
		this.suppliedPredicate = suppliedPredicate;

		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = updatingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		this.assignmentsByTable = new HashMap<>( updatingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> parameterResolutions
				),
				sessionFactory.getDomainModel(),
				navigablePath -> updatingTableGroup,
				executionContext.getSession()
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// segment the assignments by table-reference

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Assignment assignment = assignments.get( i );
			final List<ColumnReference> assignmentColumnRefs = assignment.getAssignable().getColumnReferences();

			TableReference assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final TableReference tableReference = resolveTableReference(
						columnReference,
						updatingTableGroup,
						tableReferenceByAlias
				);

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new IllegalStateException( "Assignment referred to columns from multiple tables" );
				}

				assignmentTableReference = tableReference;
			}

			List<Assignment> assignmentsForTable = assignmentsByTable.get( assignmentTableReference );
			if ( assignmentsForTable == null ) {
				assignmentsForTable = new ArrayList<>();
				assignmentsByTable.put( assignmentTableReference, assignmentsForTable );
			}
			assignmentsForTable.add( assignment );
		}
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		ExecuteWithIdTableHelper.performBeforeIdTableUseActions(
				beforeUseAction,
				idTable,
				idTableExporterAccess,
				ddlTransactionHandling,
				executionContext
		);

		try {
			final int rows = ExecuteWithIdTableHelper.saveMatchingIdsIntoIdTable(
					sqmConverter,
					suppliedPredicate,
					idTable,
					sessionUidAccess,
					jdbcParameterBindings,
					executionContext
			);

			final QuerySpec idTableSubQuery = ExecuteWithIdTableHelper.createIdTableSelectQuerySpec(
					idTable,
					sessionUidAccess,
					entityDescriptor,
					executionContext
			);

			entityDescriptor.visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnVisitationSupplier) -> updateTable(
							tableExpression,
							tableKeyColumnVisitationSupplier,
							idTableSubQuery,
							executionContext
					)
			);

			return rows;
		}
		finally {
			ExecuteWithIdTableHelper.performAfterIdTableUseActions(
					afterUseAction,
					idTable,
					idTableExporterAccess,
					ddlTransactionHandling,
					sessionUidAccess,
					executionContext
			);
		}
	}

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			TableGroup updatingTableGroup,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		final TableReference tableReferenceByName = updatingTableGroup.resolveTableReference( columnReference.getQualifier() );
		if ( tableReferenceByName != null ) {
			return tableReferenceByName;
		}

		throw new IllegalStateException( "Could not resolve restricted column's table-reference" );
	}

	private void updateTable(
			String tableExpression,
			Supplier<Consumer<ColumnConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.resolveTableReference( tableExpression );

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( assignments == null || assignments.isEmpty() ) {
			// no assignments for this table - skip it
			return;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the in-subquery predicate to restrict the updates to just
		// matching ids

		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );

		tableKeyColumnVisitationSupplier.get().accept(
				(columnExpression, containingTableExpression, jdbcMapping) -> {
					assert containingTableExpression.equals( tableExpression );
					keyColumnCollector.apply(
							new ColumnReference(
									(String) null,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
				}
		);

		final InSubQueryPredicate idTableSubQueryPredicate = new InSubQueryPredicate(
				keyColumnCollector.buildKeyExpression(),
				idTableSubQuery,
				false
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final UpdateStatement sqlAst = new UpdateStatement( updatingTableReference, assignments, idTableSubQueryPredicate );

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final SqlAstUpdateTranslator sqlAstTranslator = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildUpdateTranslator( sessionFactory );

		final JdbcUpdate jdbcUpdate = sqlAstTranslator.translate( sqlAst );

		jdbcServices.getJdbcMutationExecutor().execute(
				jdbcUpdate,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}
}
