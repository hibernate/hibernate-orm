/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class UpdateExecutionDelegate implements TableBasedUpdateHandler.ExecutionDelegate {
	private final SqmUpdateStatement<?> sqmUpdate;
	private final MultiTableSqmMutationConverter sqmConverter;
	private final TemporaryTable idTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final TableGroup updatingTableGroup;
	private final Predicate suppliedPredicate;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;

	private final Map<TableReference, List<Assignment>> assignmentsByTable;
	private final Map<SqmParameter<?>, MappingModelExpressable<?>> paramTypeResolutions;
	private final SessionFactoryImplementor sessionFactory;

	public UpdateExecutionDelegate(
			SqmUpdateStatement<?> sqmUpdate,
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup updatingTableGroup,
			TableReference hierarchyRootTableReference,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			Predicate suppliedPredicate,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> parameterResolutions,
			Map<SqmParameter<?>, MappingModelExpressable<?>> paramTypeResolutions,
			DomainQueryExecutionContext executionContext) {
		this.sqmUpdate = sqmUpdate;
		this.sqmConverter = sqmConverter;
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;
		this.updatingTableGroup = updatingTableGroup;
		this.suppliedPredicate = suppliedPredicate;
		this.paramTypeResolutions = paramTypeResolutions;

		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = updatingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		this.assignmentsByTable = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> parameterResolutions
				),
				sessionFactory.getDomainModel(),
				navigablePath -> updatingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressable<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressable<T>) paramTypeResolutions.get(parameter);
					}
				},
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
						tableReferenceByAlias
				);

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new SemanticException( "Assignment referred to columns from multiple tables: " + assignment.getAssignable() );
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
		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				idTable,
				executionContext
		);

		try {
			final int rows = ExecuteWithTemporaryTableHelper.saveMatchingIdsIntoIdTable(
					sqmConverter,
					suppliedPredicate,
					idTable,
					sessionUidAccess,
					jdbcParameterBindings,
					executionContext
			);

			final QuerySpec idTableSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
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
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					idTable,
					sessionUidAccess,
					afterUseAction,
					executionContext
			);
		}
	}

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	private NamedTableReference resolveUnionTableReference(
			TableReference tableReference,
			String tableExpression) {
		if ( tableReference instanceof UnionTableReference ) {
			return new NamedTableReference(
					tableExpression,
					tableReference.getIdentificationVariable(),
					tableReference.isOptional(),
					sessionFactory
			);
		}
		return (NamedTableReference) tableReference;
	}

	private void updateTable(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true,
				true
		);

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
				(columnIndex, selection) -> {
					assert selection.getContainingTableExpression().equals( tableExpression );
					keyColumnCollector.apply(
							new ColumnReference(
									(String) null,
									selection,
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
		final UpdateStatement sqlAst = new UpdateStatement(
				resolveUnionTableReference( updatingTableReference, tableExpression ),
				assignments,
				idTableSubQueryPredicate
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcUpdate jdbcUpdate = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildUpdateTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

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
