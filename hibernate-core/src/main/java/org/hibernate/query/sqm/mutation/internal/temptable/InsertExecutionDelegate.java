/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcUpdate;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.descriptor.ValueBinder;

/**
 * @author Steve Ebersole
 */
public class InsertExecutionDelegate implements TableBasedInsertHandler.ExecutionDelegate {
	private final SqmInsertStatement<?> sqmInsert;
	private final MultiTableSqmMutationConverter sqmConverter;
	private final TemporaryTable entityTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final TableGroup updatingTableGroup;
	private final InsertStatement insertStatement;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;

	private final Map<TableReference, List<Assignment>> assignmentsByTable;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions;
	private final SessionFactoryImplementor sessionFactory;

	public InsertExecutionDelegate(
			SqmInsertStatement<?> sqmInsert,
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable entityTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup insertingTableGroup,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			InsertStatement insertStatement,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> parameterResolutions,
			Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions,
			DomainQueryExecutionContext executionContext) {
		this.sqmInsert = sqmInsert;
		this.sqmConverter = sqmConverter;
		this.entityTable = entityTable;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;
		this.updatingTableGroup = insertingTableGroup;
		this.paramTypeResolutions = paramTypeResolutions;
		this.insertStatement = insertStatement;

		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = insertingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		this.assignmentsByTable = CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> parameterResolutions
				),
				sessionFactory.getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> insertingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) paramTypeResolutions.get(parameter);
					}
				}
				,
				executionContext.getSession()
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// segment the assignments by table-reference

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Assignment assignment = assignments.get( i );
			final Assignable assignable = assignment.getAssignable();
			final List<ColumnReference> assignmentColumnRefs = assignable.getColumnReferences();

			TableReference assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final TableReference tableReference = resolveTableReference(
						columnReference,
						insertingTableGroup,
						tableReferenceByAlias
				);

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new SemanticException( "Assignment referred to columns from multiple tables: " + i );
				}

				assignmentTableReference = tableReference;
			}

			assignmentsByTable.computeIfAbsent( assignmentTableReference, k -> new ArrayList<>() )
					.add( assignment );
		}
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		// NOTE: we could get rid of using a temporary table if the expressions in Values are "stable".
		// But that is a non-trivial optimization that requires more effort
		// as we need to split out individual inserts if we have a non-bulk capable optimizer
		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				entityTable,
				executionContext
		);

		try {
			final int rows = ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable(
					insertStatement,
					jdbcParameterBindings,
					executionContext
			);

			if ( rows != 0 ) {
				final AbstractEntityPersister persister = (AbstractEntityPersister) entityDescriptor.getEntityPersister();
				final int tableSpan = persister.getTableSpan();
				insertRootTable( persister.getTableName( 0 ), rows, persister.getKeyColumns( 0 ), executionContext );

				if ( persister.hasDuplicateTables() ) {
					final String[] insertedTables = new String[tableSpan];
					insertedTables[0] = persister.getTableName( 0 );
					for ( int i = 1; i < tableSpan; i++ ) {
						if ( persister.isInverseTable( i ) ) {
							continue;
						}
						final String tableName = persister.getTableName( i );
						insertedTables[i] = tableName;
						if ( ArrayHelper.indexOf( insertedTables, i, tableName ) != -1 ) {
							// Since secondary tables could appear multiple times, we have to skip duplicates
							continue;
						}
						insertTable(
								tableName,
								persister.getKeyColumns( i ),
								persister.isNullableTable( i ),
								executionContext
						);
					}
				}
				else {
					for ( int i = 1; i < tableSpan; i++ ) {
						insertTable(
								persister.getTableName( i ),
								persister.getKeyColumns( i ),
								persister.isNullableTable( i ),
								executionContext
						);
					}
				}
			}

			return rows;
		}
		finally {
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					entityTable,
					sessionUidAccess,
					afterUseAction,
					executionContext
			);
		}
	}

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			TableGroup updatingTableGroup,
			Map<String, TableReference> tableReferenceByAlias) {
		if ( columnReference.getQualifier() == null ) {
			// This happens only for the special row_number column
			return null;
		}
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	private NamedTableReference resolveUnionTableReference(TableReference tableReference, String tableExpression) {
		if ( tableReference instanceof UnionTableReference ) {
			return new NamedTableReference(
					tableExpression,
					tableReference.getIdentificationVariable(),
					tableReference.isOptional(),
					sessionFactory
			);
		}
		else {
			return (NamedTableReference) tableReference;
		}
	}

	private void insertRootTable(
			String tableExpression,
			int rows,
			String[] keyColumns,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true,
				true
		);

		final IdentifierGenerator identifierGenerator = entityDescriptor.getEntityPersister().getIdentifierGenerator();
		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( ( assignments == null || assignments.isEmpty() ) && !( identifierGenerator instanceof PostInsertIdentifierGenerator ) ) {
			throw new IllegalStateException( "There must be at least a single root table assignment" );
		}

		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final QuerySpec querySpec = new QuerySpec( true );
		final NamedTableReference temporaryTableReference = new NamedTableReference(
				insertStatement.getTargetTable().getTableExpression(),
				updatingTableReference.getIdentificationVariable(),
				false,
				sessionFactory
		);
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				entityDescriptor
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		final InsertStatement insertStatement = new InsertStatement( dmlTableReference );
		insertStatement.setSourceSelectStatement( querySpec );
		for ( Assignment assignment : assignments ) {
			insertStatement.addTargetColumnReferences( assignment.getAssignable().getColumnReferences() );
			for ( ColumnReference columnReference : assignment.getAssignable().getColumnReferences() ) {
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								1,
								0,
								new ColumnReference(
										updatingTableReference.getIdentificationVariable(),
										columnReference.getColumnExpression(),
										false,
										null,
										null,
										columnReference.getJdbcMapping(),
										sessionFactory
								)
						)
				);
			}
		}
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final Map<Object, Object> entityTableToRootIdentity;
		if ( identifierGenerator instanceof PostInsertIdentifierGenerator ) {
			final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			final QuerySpec idSelectQuerySpec = new QuerySpec( true );
			idSelectQuerySpec.getFromClause().addRoot( temporaryTableGroup );
			final ColumnReference columnReference = new ColumnReference(
					(String) null,
					TemporaryTable.ENTITY_TABLE_IDENTITY_COLUMN,
					false,
					null,
					null,
					identifierMapping.getJdbcMapping(),
					sessionFactory
			);
			idSelectQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 1, 0, columnReference ) );
			idSelectQuerySpec.addSortSpecification(
					new SortSpecification(
							columnReference,
							SortOrder.ASCENDING
					)
			);
			final SelectStatement selectStatement = new SelectStatement(
					idSelectQuerySpec,
					Collections.singletonList(
							new BasicFetch<>(
									0,
									null,
									null,
									identifierMapping,
									null,
									FetchTiming.IMMEDIATE,
									null
							)
					)
			);
			final JdbcSelect jdbcSelect = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildSelectTranslator( sessionFactory, selectStatement )
					.translate( null, executionContext.getQueryOptions() );
			final List<Object> list = jdbcServices.getJdbcSelectExecutor().list(
					jdbcSelect,
					JdbcParameterBindings.NO_BINDINGS,
					executionContext,
					null,
					ListResultsConsumer.UniqueSemantic.NONE
			);
			entityTableToRootIdentity = new LinkedHashMap<>( list.size() );
			for ( Object o : list ) {
				entityTableToRootIdentity.put( o, null );
			}

			querySpec.applyPredicate(
					new ComparisonPredicate(
							columnReference,
							ComparisonOperator.EQUAL,
							new JdbcParameterImpl( identifierMapping.getJdbcMapping() )
					)
			);
		}
		else {
			entityTableToRootIdentity = null;
			if ( identifierGenerator instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
				// If the generator uses an optimizer, we have to generate the identifiers for the new rows
				// but only if the target paths don't already contain the id
				if ( optimizer != null && optimizer.getIncrementSize() > 1 && insertStatement.getTargetColumnReferences()
						.stream()
						.noneMatch( c -> keyColumns[0].equals( c.getColumnExpression() ) ) ) {
					final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();

					final JdbcParameter rowNumber = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
					final JdbcParameter rootIdentity = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
					final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
					final ColumnReference idColumnReference = new ColumnReference(
							(String) null,
							identifierMapping,
							sessionFactory
					);
					temporaryTableAssignments.add(
							new Assignment(
									idColumnReference,
									rootIdentity
							)
					);
					final TemporaryTableColumn rowNumberColumn = entityTable.getColumns().get(
							entityTable.getColumns().size() - 1
					);
					final UpdateStatement updateStatement = new UpdateStatement(
							temporaryTableReference,
							temporaryTableAssignments,
							new ComparisonPredicate(
									new ColumnReference(
											(String) null,
											rowNumberColumn.getColumnName(),
											false,
											null,
											null,
											rowNumberColumn.getJdbcMapping(),
											sessionFactory
									),
									ComparisonOperator.EQUAL,
									rowNumber
							)
					);

					final JdbcUpdate jdbcUpdate = jdbcServices.getJdbcEnvironment()
							.getSqlAstTranslatorFactory()
							.buildUpdateTranslator( sessionFactory, updateStatement )
							.translate( null, executionContext.getQueryOptions() );
					final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 2 );

					for ( int i = 0; i < rows; i++ ) {
						updateBindings.addBinding(
								rowNumber,
								new JdbcParameterBindingImpl(
										rowNumberColumn.getJdbcMapping(),
										i + 1
								)
						);
						updateBindings.addBinding(
								rootIdentity,
								new JdbcParameterBindingImpl(
										identifierMapping.getJdbcMapping(),
										identifierGenerator.generate(
												executionContext.getSession(),
												null
										)
								)
						);
						jdbcServices.getJdbcMutationExecutor().execute(
								jdbcUpdate,
								updateBindings,
								sql -> executionContext.getSession()
										.getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql ),
								(integer, preparedStatement) -> {
								},
								executionContext
						);
					}

					insertStatement.addTargetColumnReferences(
							new ColumnReference(
									(String) null,
									keyColumns[0],
									false,
									null,
									null,
									identifierMapping.getJdbcMapping(),
									sessionFactory
							)
					);
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									1,
									0,
									new ColumnReference(
											updatingTableReference.getIdentificationVariable(),
											idColumnReference.getColumnExpression(),
											false,
											null,
											null,
											idColumnReference.getJdbcMapping(),
											sessionFactory
									)
							)
					);
				}
			}
		}

		final JdbcInsert jdbcInsert = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildInsertTranslator( sessionFactory, insertStatement )
				.translate( null, executionContext.getQueryOptions() );

		if ( identifierGenerator instanceof PostInsertIdentifierGenerator ) {
			final PostInsertIdentifierGenerator generator = (PostInsertIdentifierGenerator) identifierGenerator;
			final boolean generatedKeysEnabled = sessionFactory.getSessionFactoryOptions().isGetGeneratedKeysEnabled();
			final InsertGeneratedIdentifierDelegate identifierDelegate = generator.getInsertGeneratedIdentifierDelegate(
					(PostInsertIdentityPersister) entityDescriptor.getEntityPersister(),
					jdbcServices.getDialect(),
					generatedKeysEnabled
			);
			final String finalSql = identifierDelegate.prepareIdentifierGeneratingInsert( jdbcInsert.getSql() );
			final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			final ValueBinder jdbcValueBinder = identifierMapping.getJdbcMapping().getJdbcValueBinder();
			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				final Object rootIdentity = identifierDelegate.performInsert(
						finalSql,
						executionContext.getSession(),
						new Binder() {
							@Override
							public void bindValues(PreparedStatement ps) throws SQLException {
								jdbcValueBinder.bind( ps, entry.getKey(), 1, executionContext.getSession() );
							}

							@Override
							public Object getEntity() {
								return null;
							}
						}
				);
				entry.setValue( rootIdentity );
			}

			final JdbcParameter entityIdentity = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
			final JdbcParameter rootIdentity = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
			final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
			temporaryTableAssignments.add(
					new Assignment(
							new ColumnReference(
									(String) null,
									identifierMapping,
									sessionFactory
							),
							rootIdentity
					)
			);
			final UpdateStatement updateStatement = new UpdateStatement(
					temporaryTableReference,
					temporaryTableAssignments,
					new ComparisonPredicate(
							new ColumnReference(
									(String) null,
									TemporaryTable.ENTITY_TABLE_IDENTITY_COLUMN,
									false,
									null,
									null,
									identifierMapping.getJdbcMapping(),
									sessionFactory
							),
							ComparisonOperator.EQUAL,
							entityIdentity
					)
			);

			final JdbcUpdate jdbcUpdate = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildUpdateTranslator( sessionFactory, updateStatement )
					.translate( null, executionContext.getQueryOptions() );
			final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 2 );

			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				updateBindings.addBinding( entityIdentity, new JdbcParameterBindingImpl( identifierMapping.getJdbcMapping(), entry.getKey() ) );
				updateBindings.addBinding( rootIdentity, new JdbcParameterBindingImpl( identifierMapping.getJdbcMapping(), entry.getValue() ) );
				jdbcServices.getJdbcMutationExecutor().execute(
						jdbcUpdate,
						updateBindings,
						sql -> executionContext.getSession()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql ),
						(integer, preparedStatement) -> {
						},
						executionContext
				);
			}
		}
		else {
			jdbcServices.getJdbcMutationExecutor().execute(
					jdbcInsert,
					JdbcParameterBindings.NO_BINDINGS,
					sql -> executionContext.getSession()
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql ),
					(integer, preparedStatement) -> {
					},
					executionContext
			);
		}
	}

	private void insertTable(
			String tableExpression,
			String[] keyColumns,
			boolean nullableTable,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true,
				true
		);

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( nullableTable && ( assignments == null || assignments.isEmpty() ) ) {
			// no assignments for this table - skip it
			return;
		}
		final NamedTableReference dmlTargetTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		final QuerySpec querySpec = new QuerySpec( true );
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				new NamedTableReference(
						insertStatement.getTargetTable().getTableExpression(),
						updatingTableReference.getIdentificationVariable(),
						false,
						sessionFactory
				),
				entityDescriptor
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		final InsertStatement insertStatement = new InsertStatement( dmlTargetTableReference );
		insertStatement.setSourceSelectStatement( querySpec );
		if ( assignments != null && !assignments.isEmpty() ) {
			for ( Assignment assignment : assignments ) {
				insertStatement.addTargetColumnReferences( assignment.getAssignable().getColumnReferences() );
				for ( ColumnReference columnReference : assignment.getAssignable().getColumnReferences() ) {
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									1,
									0,
									new ColumnReference(
											updatingTableReference.getIdentificationVariable(),
											columnReference.getColumnExpression(),
											false,
											null,
											null,
											columnReference.getJdbcMapping(),
											sessionFactory
									)
							)
					);
				}
			}
		}
		final String targetKeyColumnName = keyColumns[0];
		final AbstractEntityPersister entityPersister = (AbstractEntityPersister) entityDescriptor.getEntityPersister();
		final IdentifierGenerator identifierGenerator = entityPersister.getIdentifierGenerator();
		final boolean needsKeyInsert;
		if ( identifierGenerator instanceof PostInsertIdentifierGenerator ) {
			needsKeyInsert = true;
		}
		else if ( identifierGenerator instanceof OptimizableGenerator ) {
			final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
			// If the generator uses an optimizer, we have to generate the identifiers for the new rows
			needsKeyInsert = optimizer != null && optimizer.getIncrementSize() > 1;
		}
		else {
			needsKeyInsert = true;
		}
		if ( needsKeyInsert && insertStatement.getTargetColumnReferences()
				.stream()
				.noneMatch( c -> targetKeyColumnName.equals( c.getColumnExpression() ) ) ) {
			final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			insertStatement.addTargetColumnReferences(
					new ColumnReference(
							dmlTargetTableReference.getIdentificationVariable(),
							targetKeyColumnName,
							false,
							null,
							null,
							identifierMapping.getJdbcMapping(),
							sessionFactory
					)
			);
			querySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							1,
							0,
							new ColumnReference(
									updatingTableReference.getIdentificationVariable(),
									identifierMapping,
									sessionFactory
							)
					)
			);
		}
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcInsert jdbcInsert = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildInsertTranslator( sessionFactory, insertStatement )
				.translate( null, executionContext.getQueryOptions() );

		jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				JdbcParameterBindings.NO_BINDINGS,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
				executionContext
		);
	}
}
