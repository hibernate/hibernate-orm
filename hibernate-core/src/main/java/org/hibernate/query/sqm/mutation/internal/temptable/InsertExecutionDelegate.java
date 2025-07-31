/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.results.internal.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
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
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.descriptor.ValueBinder;

import static org.hibernate.generator.EventType.INSERT;

/**
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class InsertExecutionDelegate implements TableBasedInsertHandler.ExecutionDelegate {
	private final TemporaryTable entityTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final TableGroup updatingTableGroup;
	private final InsertSelectStatement insertStatement;
	private final ConflictClause conflictClause;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;
	private final JdbcParameter sessionUidParameter;

	private final boolean assignsId;
	private final Map<String, List<Assignment>> assignmentsByTable;
	private final SessionFactoryImplementor sessionFactory;

	public InsertExecutionDelegate(
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable entityTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup insertingTableGroup,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			boolean assignsId,
			InsertSelectStatement insertStatement,
			ConflictClause conflictClause,
			JdbcParameter sessionUidParameter,
			DomainQueryExecutionContext executionContext) {
		this.entityTable = entityTable;
		this.temporaryTableStrategy = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;
		this.sessionUidAccess = sessionUidAccess;
		this.updatingTableGroup = insertingTableGroup;
		this.conflictClause = conflictClause;
		this.sessionUidParameter = sessionUidParameter;
		this.insertStatement = insertStatement;

		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = insertingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		this.assignmentsByTable = CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter ),
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmConverter.getSqmParameterMappingModelExpressibleResolutions().get( parameter );
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
				final TableReference tableReference = resolveTableReference( columnReference, tableReferenceByAlias );

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new SemanticException( "Assignment referred to columns from multiple tables: " + i );
				}
				assignmentTableReference = tableReference;
			}

			assignmentsByTable.computeIfAbsent(
					assignmentTableReference == null ? null : assignmentTableReference.getTableId(),
					k -> new ArrayList<>() ).add( assignment );
		}

		this.assignsId = assignsId;
	}

	private List<TemporaryTableColumn> getPrimaryKeyTableColumns(EntityPersister entityPersister, TemporaryTable entityTable) {
		final boolean identityColumn = entityPersister.getGenerator().generatedOnExecution();
		final int startIndex = identityColumn ? 1 : 0;
		final int endIndex = startIndex + entityPersister.getIdentifierMapping().getJdbcTypeCount();
		return entityTable.getColumns().subList( startIndex, endIndex );
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		// NOTE: we could get rid of using a temporary table if the expressions in Values are "stable".
		// But that is a non-trivial optimization that requires more effort
		// as we need to split out individual inserts if we have a non-bulk capable optimizer
		final boolean createdTable = ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				entityTable,
				temporaryTableStrategy,
				executionContext
		);

		try {
			if ( sessionUidParameter != null ) {
				jdbcParameterBindings.addBinding(
						sessionUidParameter,
						new JdbcParameterBindingImpl(
								entityTable.getSessionUidColumn().getJdbcMapping(),
								UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) )
						)
				);
			}
			final int rows = ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable(
					insertStatement,
					jdbcParameterBindings,
					executionContext
			);

			if ( rows != 0 ) {
				final EntityPersister 	persister = entityDescriptor.getEntityPersister();
				final int tableSpan = persister.getTableSpan();
				final int insertedRows = insertRootTable(
						persister.getTableName( 0 ),
						rows,
						createdTable,
						persister.getKeyColumns( 0 ),
						executionContext
				);

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
				return insertedRows;
			}

			return rows;
		}
		finally {
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					entityTable,
					sessionUidAccess,
					forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction(),
					executionContext
			);
		}
	}

	private TableReference resolveTableReference(
			ColumnReference columnReference,
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
					tableReference.isOptional()
			);
		}
		else {
			return (NamedTableReference) tableReference;
		}
	}

	private int insertRootTable(
			String tableExpression,
			int rows,
			boolean rowNumberStartsAtOne,
			String[] keyColumns,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		final Generator generator = entityPersister.getGenerator();
		final List<Assignment> assignments = assignmentsByTable.get( tableExpression );
		if ( !assignsId
			&& (assignments == null || assignments.isEmpty())
			&& !generator.generatedOnExecution()
			&& (!(generator instanceof BulkInsertionCapableIdentifierGenerator)
				|| ((BulkInsertionCapableIdentifierGenerator) generator).supportsBulkInsertionIdentifierGeneration()) ) {
			throw new IllegalStateException( "There must be at least a single root table assignment" );
		}

		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final QuerySpec querySpec = new QuerySpec( true );
		final NamedTableReference temporaryTableReference = new NamedTableReference(
				insertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				entityDescriptor
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		if ( insertStatement.getValuesList().size() == 1 && conflictClause != null ) {
			// Potentially apply a limit 1 to allow the use of the conflict clause emulation
			querySpec.setFetchClauseExpression(
					new QueryLiteral<>(
							1,
							executionContext.getSession().getFactory().getQueryEngine().getCriteriaBuilder().getIntegerType()
					),
					FetchClauseType.ROWS_ONLY
			);
		}
		final InsertSelectStatement insertStatement = new InsertSelectStatement( dmlTableReference );
		insertStatement.setConflictClause( conflictClause );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference );
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final Map<Object, Object> entityTableToRootIdentity;
		final SharedSessionContractImplementor session = executionContext.getSession();
		if ( !assignsId && generator.generatedOnExecution() ) {
			final BasicEntityIdentifierMapping identifierMapping =
					(BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			final QuerySpec idSelectQuerySpec = new QuerySpec( true );
			idSelectQuerySpec.getFromClause().addRoot( temporaryTableGroup );
			final ColumnReference columnReference = new ColumnReference(
					(String) null,
					TemporaryTable.ENTITY_TABLE_IDENTITY_COLUMN,
					false,
					null,
					identifierMapping.getJdbcMapping()
			);
			idSelectQuerySpec.getSelectClause()
					.addSqlSelection( new SqlSelectionImpl( 0, columnReference ) );
			idSelectQuerySpec.addSortSpecification(
					new SortSpecification(
							columnReference,
							SortDirection.ASCENDING
					)
			);
			if ( entityTable.getSessionUidColumn() != null ) {
				final TemporaryTableColumn sessionUidColumn = entityTable.getSessionUidColumn();
				idSelectQuerySpec.applyPredicate( new ComparisonPredicate(
						new ColumnReference(
								temporaryTableReference,
								sessionUidColumn.getColumnName(),
								false,
								null,
								sessionUidColumn.getJdbcMapping()
						),
						ComparisonOperator.EQUAL,
						sessionUidParameter
				) );
			}
			final SelectStatement selectStatement = new SelectStatement(
					idSelectQuerySpec,
					Collections.singletonList(
							new BasicFetch<>(
									0,
									null,
									null,
									identifierMapping,
									FetchTiming.IMMEDIATE,
									null,
									false
							)
					)
			);
			final JdbcOperationQuerySelect jdbcSelect = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildSelectTranslator( sessionFactory, selectStatement )
					.translate( null, executionContext.getQueryOptions() );
			final List<Object> list = jdbcServices.getJdbcSelectExecutor().list(
					jdbcSelect,
					jdbcParameterBindings,
					executionContext,
					null,
					null,
					ListResultsConsumer.UniqueSemantic.NONE,
					rows
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
			// if the target paths don't already contain the id, and we need identifier generation,
			// then we load update rows from the temporary table with the generated identifiers,
			// to then insert into the target tables in once statement
			if ( insertStatement.getTargetColumns().stream()
							.noneMatch( c -> keyColumns[0].equals( c.getColumnExpression() ) ) ) {
				final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
				final List<TemporaryTableColumn> primaryKeyTableColumns =
						getPrimaryKeyTableColumns( entityPersister, entityTable );

				final TemporaryTableColumn sessionUidColumn;
				final Predicate sessionUidPredicate;
				if ( entityTable.getSessionUidColumn() == null ) {
					sessionUidColumn = null;
					sessionUidPredicate = null;
				}
				else {
					sessionUidColumn = entityTable.getSessionUidColumn();
					sessionUidPredicate = new ComparisonPredicate(
							new ColumnReference(
									(String) null,
									sessionUidColumn.getColumnName(),
									false,
									null,
									sessionUidColumn.getJdbcMapping()
							),
							ComparisonOperator.EQUAL,
							sessionUidParameter
					);
				}
				if ( needsIdentifierGeneration( generator ) ) {
					final BasicEntityIdentifierMapping basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
					final JdbcParameter rootIdentity = new JdbcParameterImpl( basicIdentifierMapping.getJdbcMapping() );
					final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
					final ColumnReference idColumnReference = new ColumnReference( (String) null, basicIdentifierMapping );
					temporaryTableAssignments.add( new Assignment( idColumnReference, rootIdentity ) );

					final JdbcParameter rowNumber = new JdbcParameterImpl( basicIdentifierMapping.getJdbcMapping() );
					final int rowNumberIndex =
							entityTable.getColumns().size() - (entityTable.getSessionUidColumn() == null ? 1 : 2);
					final TemporaryTableColumn rowNumberColumn = entityTable.getColumns().get( rowNumberIndex );

					final UpdateStatement updateStatement = new UpdateStatement(
							temporaryTableReference,
							temporaryTableAssignments,
							Predicate.combinePredicates(
									new ComparisonPredicate(
											new ColumnReference(
													(String) null,
													rowNumberColumn.getColumnName(),
													false,
													null,
													rowNumberColumn.getJdbcMapping()
											),
											ComparisonOperator.EQUAL,
											rowNumber
									),
									sessionUidPredicate
							)
					);

					final JdbcOperationQueryMutation jdbcUpdate = jdbcServices.getJdbcEnvironment()
							.getSqlAstTranslatorFactory()
							.buildMutationTranslator( sessionFactory, updateStatement )
							.translate( null, executionContext.getQueryOptions() );
					final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 2 );
					if ( sessionUidColumn != null ) {
						updateBindings.addBinding(
								sessionUidParameter,
								new JdbcParameterBindingImpl(
										sessionUidColumn.getJdbcMapping(),
										UUID.fromString( sessionUidAccess.apply( session ) )
								)
						);
					}

					final BeforeExecutionGenerator beforeExecutionGenerator = (BeforeExecutionGenerator) generator;
					final IntStream rowNumberStream = !rowNumberStartsAtOne
							? IntStream.of( ExecuteWithTemporaryTableHelper.loadInsertedRowNumbers( entityTable, sessionUidAccess, rows, executionContext ) )
							: IntStream.range( 1, rows + 1 );
					rowNumberStream.forEach( rowNumberValue -> {
						updateBindings.addBinding(
								rowNumber,
								new JdbcParameterBindingImpl(
										rowNumberColumn.getJdbcMapping(),
										rowNumberValue
								)
						);
						updateBindings.addBinding(
								rootIdentity,
								new JdbcParameterBindingImpl(
										basicIdentifierMapping.getJdbcMapping(),
										beforeExecutionGenerator.generate( session, null, null, INSERT )
								)
						);
						final int updateCount = jdbcServices.getJdbcMutationExecutor().execute(
								jdbcUpdate,
								updateBindings,
								sql -> session
										.getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql ),
								(integer, preparedStatement) -> {
								},
								executionContext
						);
						assert updateCount == 1;
					} );
				}

				identifierMapping.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
					insertStatement.addTargetColumnReferences(
							new ColumnReference(
									(String) null,
									keyColumns[selectionIndex],
									false,
									null,
									selectableMapping.getJdbcMapping()
							)
					);
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									new ColumnReference(
											temporaryTableReference.getIdentificationVariable(),
											primaryKeyTableColumns.get( selectionIndex ).getColumnName(),
											false,
											null,
											selectableMapping.getJdbcMapping()
									)
							)
					);
				} );
			}
		}
		if ( entityTable.getSessionUidColumn() != null ) {
			final TemporaryTableColumn sessionUidColumn = entityTable.getSessionUidColumn();
			querySpec.applyPredicate( new ComparisonPredicate(
					new ColumnReference(
							temporaryTableReference,
							sessionUidColumn.getColumnName(),
							false,
							null,
							sessionUidColumn.getJdbcMapping()
					),
					ComparisonOperator.EQUAL,
					sessionUidParameter
			) );
		}

		final JdbcOperationQueryMutation jdbcInsert = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( sessionFactory, insertStatement )
				.translate( null, executionContext.getQueryOptions() );

		if ( !assignsId && generator.generatedOnExecution() ) {
			final GeneratedValuesMutationDelegate insertDelegate = entityDescriptor.getEntityPersister().getInsertDelegate();
			// todo 7.0 : InsertGeneratedIdentifierDelegate will be removed once we're going to handle
			//            generated values within the jdbc insert operaetion itself
			final InsertGeneratedIdentifierDelegate identifierDelegate = (InsertGeneratedIdentifierDelegate) insertDelegate;
			final String finalSql = identifierDelegate.prepareIdentifierGeneratingInsert( jdbcInsert.getSqlString() );
			final BasicEntityIdentifierMapping identifierMapping =
					(BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			final ValueBinder jdbcValueBinder = identifierMapping.getJdbcMapping().getJdbcValueBinder();
			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				final GeneratedValues generatedValues = identifierDelegate.performInsertReturning(
						finalSql,
						session,
						new Binder() {
							@Override
							public void bindValues(PreparedStatement ps) throws SQLException {
								jdbcValueBinder.bind( ps, entry.getKey(), 1, session );
								if ( sessionUidParameter != null ) {
									sessionUidParameter.getParameterBinder()
											.bindParameterValue( ps, 2, jdbcParameterBindings, executionContext );
								}
							}
							@Override
							public Object getEntity() {
								return null;
							}
						}
				);
				final Object rootIdentity = generatedValues.getGeneratedValue( identifierMapping );
				entry.setValue( rootIdentity );
			}

			final List<TemporaryTableColumn> primaryKeyTableColumns =
					getPrimaryKeyTableColumns( entityPersister, entityTable );
			assert primaryKeyTableColumns.size() == 1;

			final JdbcParameter entityIdentity = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
			final JdbcParameter rootIdentity = new JdbcParameterImpl( identifierMapping.getJdbcMapping() );
			final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
			temporaryTableAssignments.add(
					new Assignment(
							new ColumnReference(
									(String) null,
									primaryKeyTableColumns.get( 0 ).getColumnName(),
									false,
									null,
									primaryKeyTableColumns.get( 0 ).getJdbcMapping()
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
									identifierMapping.getJdbcMapping()
							),
							ComparisonOperator.EQUAL,
							entityIdentity
					)
			);

			final JdbcOperationQueryMutation jdbcUpdate = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildMutationTranslator( sessionFactory, updateStatement )
					.translate( null, executionContext.getQueryOptions() );
			final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 2 );

			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				JdbcMapping jdbcMapping = identifierMapping.getJdbcMapping();
				updateBindings.addBinding( entityIdentity, new JdbcParameterBindingImpl( jdbcMapping, entry.getKey() ) );
				updateBindings.addBinding( rootIdentity, new JdbcParameterBindingImpl( jdbcMapping, entry.getValue() ) );
				jdbcServices.getJdbcMutationExecutor().execute(
						jdbcUpdate,
						updateBindings,
						sql -> session
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql ),
						(integer, preparedStatement) -> {
						},
						executionContext
				);
			}

			return entityTableToRootIdentity.size();
		}
		else {
			return jdbcServices.getJdbcMutationExecutor().execute(
					jdbcInsert,
					jdbcParameterBindings,
					sql -> session
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql ),
					(integer, preparedStatement) -> {
					},
					executionContext
			);
		}
	}

	private boolean needsIdentifierGeneration(Generator identifierGenerator) {
		if ( !assignsId && identifierGenerator instanceof OptimizableGenerator ) {
			// If the generator uses an optimizer or is not bulk insertion capable,
			// we have to generate identifiers for the new rows, as that couldn't
			// have been done via a SQL expression
			final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
			return optimizer != null && optimizer.getIncrementSize() > 1
				|| identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator
						&& !( (BulkInsertionCapableIdentifierGenerator) identifierGenerator )
									.supportsBulkInsertionIdentifierGeneration();
		}
		else {
			return false;
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
				true
		);

		final List<Assignment> assignments = assignmentsByTable.get( tableExpression );
		if ( nullableTable && ( assignments == null || assignments.isEmpty() ) ) {
			// no assignments for this table - skip it
			return;
		}
		final NamedTableReference dmlTargetTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		final QuerySpec querySpec = new QuerySpec( true );
		final NamedTableReference temporaryTableReference = new NamedTableReference(
				insertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				entityDescriptor
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		final InsertSelectStatement insertStatement = new InsertSelectStatement( dmlTargetTableReference );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference );
		if ( insertStatement.getTargetColumns()
				.stream()
				.noneMatch( c -> keyColumns[0].equals( c.getColumnExpression() ) ) ) {
			final List<TemporaryTableColumn> primaryKeyTableColumns =
					getPrimaryKeyTableColumns( entityDescriptor.getEntityPersister(), entityTable );
			entityDescriptor.getIdentifierMapping().forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				insertStatement.addTargetColumnReferences(
						new ColumnReference(
								(String) null,
								keyColumns[selectionIndex],
								false,
								null,
								selectableMapping.getJdbcMapping()
						)
				);
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								new ColumnReference(
										temporaryTableReference.getIdentificationVariable(),
										primaryKeyTableColumns.get( selectionIndex ).getColumnName(),
										false,
										null,
										selectableMapping.getJdbcMapping()
								)
						)
				);
			} );
		}

		if ( entityTable.getSessionUidColumn() != null ) {
			final TemporaryTableColumn sessionUidColumn = entityTable.getSessionUidColumn();
			querySpec.applyPredicate( new ComparisonPredicate(
					new ColumnReference(
							temporaryTableReference,
							sessionUidColumn.getColumnName(),
							false,
							null,
							sessionUidColumn.getJdbcMapping()
					),
					ComparisonOperator.EQUAL,
					sessionUidParameter
			) );
		}
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcOperationQueryMutation jdbcInsert = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( sessionFactory, insertStatement )
				.translate( null, executionContext.getQueryOptions() );

		jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
				executionContext
		);
	}

	private void applyAssignments(List<Assignment> assignments, InsertSelectStatement insertStatement, NamedTableReference temporaryTableReference) {
		if ( assignments != null && !assignments.isEmpty() ) {
			for ( Assignment assignment : assignments ) {
				final Assignable assignable = assignment.getAssignable();
				insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
				final List<TemporaryTableColumn> columns = entityTable.findTemporaryTableColumns(
						entityDescriptor.getEntityPersister(),
						((SqmPathInterpretation<?>) assignable).getExpressionType()
				);
				for ( TemporaryTableColumn temporaryTableColumn : columns ) {
					insertStatement.getSourceSelectStatement().getFirstQuerySpec().getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									new ColumnReference(
											temporaryTableReference.getIdentificationVariable(),
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
	}
}
