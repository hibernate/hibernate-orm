/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.results.internal.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.internal.InsertHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;
import org.jboss.logging.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.util.collections.ArrayHelper.indexOf;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter.omittingLockingAndPaging;
import static org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper.createRowNumberingExpression;
import static org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper.isId;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createInsertedRowNumbersSelectSql;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createTemporaryTableInsert;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.loadInsertedRowNumbers;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable;

/**
* @author Christian Beikov
*/
public class TableBasedInsertHandler extends AbstractMutationHandler implements InsertHandler {
	private static final Logger LOG = Logger.getLogger( TableBasedInsertHandler.class );

	private final TemporaryTable entityTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;
	private final @Nullable JdbcParameter sessionUidParameter;

	private final CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> temporaryTableInsert;
	private final RootTableInserter rootTableInserter;
	private final List<JdbcOperationQueryMutation> nonRootTableInserts;

	public TableBasedInsertHandler(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref,
			TemporaryTable entityTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( sqmInsert, context.getSession().getSessionFactory() );
		this.temporaryTableStrategy = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;
		this.entityTable = entityTable;
		this.sessionUidAccess = sessionUidAccess;

		final var sessionUidColumn = entityTable.getSessionUidColumn();
		this.sessionUidParameter =
				sessionUidColumn == null
						? null
						: new SqlTypedMappingJdbcParameter( sessionUidColumn );
		final var executionContext = omittingLockingAndPaging( context );
		final var entityDescriptor = getEntityDescriptor();
		final var sqmConverter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmInsert,
				sqmInsert.getTarget(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				getSessionFactory().getSqlTranslationEngine()
		);

		final var insertingTableGroup = sqmConverter.getMutatingTableGroup();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the insertion target using our special converter, collecting
		// information about the target paths

		final List<Assignment> targetPathColumns = new ArrayList<>();
		final var entityTableReference = new NamedTableReference(
				entityTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true
		);
		final var insertStatement = new InsertSelectStatement( entityTableReference );

		final var additionalInsertValues = sqmConverter.visitInsertionTargetPaths(
				(assignable, columnReferences) -> {
					final var pathInterpretation = (SqmPathInterpretation<?>) assignable;
					final var columns =
							entityTable.findTemporaryTableColumns( entityDescriptor,
									pathInterpretation.getExpressionType() );
					for ( var column : columns ) {
						insertStatement.addTargetColumnReference( new ColumnReference(
								entityTableReference,
								column.getColumnName(),
								column.getJdbcMapping()
						) );
					}
					targetPathColumns.add( new Assignment( assignable, (Expression) assignable ) );
				},
				sqmInsert,
				entityDescriptor,
				insertingTableGroup
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		if ( sqmInsert instanceof SqmInsertSelectStatement<?> sqmInsertSelectStatement ) {
			final var queryPart =
					sqmConverter.visitQueryPart( sqmInsertSelectStatement.getSelectQueryPart() );
			queryPart.visitQuerySpecs(
					querySpec -> {
						if ( additionalInsertValues.applySelections( querySpec, getSessionFactory() ) ) {
							final var rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
							final var columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns()
									.set( insertStatement.getTargetColumns().size() - 1,
											columnReference );
							targetPathColumns.set( targetPathColumns.size() - 1,
									new Assignment( columnReference, columnReference ) );
						}
						else if ( !( entityDescriptor.getGenerator() instanceof OnExecutionGenerator generator
										&& generator.generatedOnExecution() )
								&& !entityTable.isRowNumberGenerated() ) {
							final var rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
							final var columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns().add( columnReference );
							targetPathColumns.add( new Assignment( columnReference, columnReference ) );
							querySpec.getSelectClause()
									.addSqlSelection( new SqlSelectionImpl(0,
											createRowNumberingExpression( querySpec, getSessionFactory() ) ) );
						}
						if ( sessionUidColumn != null ) {
							final var sessionUidColumnReference = new ColumnReference(
									(String) null,
									sessionUidColumn.getColumnName(),
									false,
									null,
									sessionUidColumn.getJdbcMapping()
							);
							querySpec.getSelectClause()
									.addSqlSelection( new SqlSelectionImpl(
											insertStatement.getTargetColumns().size(),
											sessionUidParameter
									) );
							insertStatement.getTargetColumns().add( sessionUidColumnReference );
							targetPathColumns.add( new Assignment( sessionUidColumnReference, sessionUidParameter ) );
						}
					}
			);
			insertStatement.setSourceSelectStatement( queryPart );
		}
		else {
			// Add the row number column if there is one
			final BasicType<?> rowNumberType;
			if ( !( entityDescriptor.getGenerator() instanceof OnExecutionGenerator generator
						&& generator.generatedOnExecution() )
				&& !entityTable.isRowNumberGenerated() ) {
				final var rowNumberColumn = entityTable.getColumns()
						.get( entityTable.getColumns().size() - (sessionUidColumn == null ? 1 : 2) );
				rowNumberType = (BasicType<?>) rowNumberColumn.getJdbcMapping();
				final var columnReference = new ColumnReference(
						(String) null,
						rowNumberColumn.getColumnName(),
						false,
						null,
						rowNumberColumn.getJdbcMapping()
				);
				insertStatement.getTargetColumns().add( columnReference );
				targetPathColumns.add( new Assignment( columnReference, columnReference ) );
			}
			else {
				rowNumberType = null;
			}
			if ( sessionUidColumn != null ) {
				final var sessionUidColumnReference = new ColumnReference(
						(String) null,
						sessionUidColumn.getColumnName(),
						false,
						null,
						sessionUidColumn.getJdbcMapping()
				);
				insertStatement.getTargetColumns().add( sessionUidColumnReference );
				targetPathColumns.add( new Assignment( sessionUidColumnReference, sessionUidParameter ) );
			}
			final var sqmValuesList = ( (SqmInsertValuesStatement<?>) sqmInsert ).getValuesList();
			final List<Values> valuesList = new ArrayList<>( sqmValuesList.size() );
			for ( int i = 0; i < sqmValuesList.size(); i++ ) {
				final var values = sqmConverter.visitValues( sqmValuesList.get( i ) );
				additionalInsertValues.applyValues( values );
				if ( rowNumberType != null ) {
					values.getExpressions().add( new QueryLiteral<>(
							rowNumberType.getJavaTypeDescriptor()
									.wrap( i + 1, getSessionFactory().getWrapperOptions() ),
							rowNumberType
					) );
				}
				if ( sessionUidParameter != null ) {
					values.getExpressions().add( sessionUidParameter );
				}
				valuesList.add( values );
			}
			insertStatement.setValuesList( valuesList );
		}
		final var conflictClause = sqmConverter.visitConflictClause( sqmInsert.getConflictClause() );
		sqmConverter.pruneTableGroupJoins();

		boolean assignsId = false;
		for ( var assignment : targetPathColumns ) {
			assignsId = assignsId
						|| assignment.getAssignable() instanceof SqmPathInterpretation<?> pathInterpretation
								&& isId( pathInterpretation.getExpressionType() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias =
				mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( insertingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < insertingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( insertingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final var updatingModelPart = insertingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		final Map<String, List<Assignment>> assignmentsByTable =
				mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );

		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter );
		this.resolvedParameterMappingModelTypes = sqmConverter.getSqmParameterMappingModelExpressibleResolutions();

		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							entityTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// segment the assignments by table-reference

		for ( int i = 0; i < targetPathColumns.size(); i++ ) {
			final var assignment = targetPathColumns.get( i );
			final var assignable = assignment.getAssignable();
			final var assignmentColumnRefs = assignable.getColumnReferences();

			TableReference assignmentTableReference = null;
			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final var columnReference = assignmentColumnRefs.get( c );
				final var tableReference = resolveTableReference( columnReference, tableReferenceByAlias );

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new SemanticException( "Assignment referred to columns from multiple tables: " + i );
				}
				assignmentTableReference = tableReference;
			}

			assignmentsByTable.computeIfAbsent(
					assignmentTableReference == null
							? null
							: assignmentTableReference.getTableId(),
					k -> new ArrayList<>() ).add( assignment );
		}

		this.temporaryTableInsert =
				createTemporaryTableInsert(
						insertStatement,
						jdbcParameterBindings,
						executionContext
				);

		final int tableSpan = entityDescriptor.getTableSpan();
		this.rootTableInserter = createRootTableInserter(
				insertStatement,
				insertingTableGroup,
				conflictClause,
				assignsId,
				entityDescriptor.getTableName( 0 ),
				entityDescriptor.getKeyColumns( 0 ),
				assignmentsByTable,
				executionContext
		);

		final ArrayList<JdbcOperationQueryMutation> nonRootTableInserts = new ArrayList<>( tableSpan );
		if ( entityDescriptor.hasDuplicateTables() ) {
			final var insertedTables = new String[tableSpan];
			insertedTables[0] = entityDescriptor.getTableName( 0 );
			for ( int i = 1; i < tableSpan; i++ ) {
				if ( entityDescriptor.isInverseTable( i ) ) {
					continue;
				}
				final String tableName = entityDescriptor.getTableName( i );
				insertedTables[i] = tableName;
				if ( indexOf( insertedTables, i, tableName ) != -1 ) {
					// Since secondary tables could appear multiple times, we have to skip duplicates
					continue;
				}
				final var insert = createTableInsert(
						insertStatement,
						insertingTableGroup,
						tableName,
						entityDescriptor.getKeyColumns( i ),
						entityDescriptor.isNullableTable( i ),
						assignmentsByTable,
						executionContext
				);
				if ( insert != null ) {
					nonRootTableInserts.add( insert );
				}
			}
		}
		else {
			for ( int i = 1; i < tableSpan; i++ ) {
				final var insert = createTableInsert(
						insertStatement,
						insertingTableGroup,
						entityDescriptor.getTableName( i ),
						entityDescriptor.getKeyColumns( i ),
						entityDescriptor.isNullableTable( i ),
						assignmentsByTable,
						executionContext
				);
				if ( insert != null ) {
					nonRootTableInserts.add( insert );
				}
			}
		}
		this.nonRootTableInserts = nonRootTableInserts;
		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
	}

	private void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	private void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
	}

	protected record RootTableInserter(
			@Nullable JdbcSelect temporaryTableIdentitySelect,
			@Nullable JdbcOperationQueryMutation temporaryTableIdUpdate,
			@Nullable String temporaryTableRowNumberSelectSql,
			JdbcOperationQueryMutation rootTableInsert,
			@Nullable String rootTableInsertWithReturningSql,
			@Nullable JdbcOperationQueryMutation temporaryTableIdentityUpdate
	) {}

	private RootTableInserter createRootTableInserter(
			InsertSelectStatement translatedInsertStatement,
			TableGroup updatingTableGroup,
			ConflictClause conflictClause,
			boolean assignsId,
			String tableExpression,
			String[] keyColumns,
			Map<String, List<Assignment>> assignmentsByTable,
			ExecutionContext executionContext) {
		final var updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final var generator = getEntityDescriptor().getGenerator();
		final var assignments = assignmentsByTable.get( tableExpression );
		if ( !assignsId
			&& ( assignments == null || assignments.isEmpty() )
			&& !generator.generatedOnExecution()
			&& ( !( generator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapable )
					|| bulkInsertionCapable.supportsBulkInsertionIdentifierGeneration() ) ) {
			throw new IllegalStateException( "There must be at least a single root table assignment" );
		}

		final var dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final var querySpec = new QuerySpec( true );
		final var temporaryTableReference = new NamedTableReference(
				translatedInsertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final var temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				getEntityDescriptor()
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		if ( translatedInsertStatement.getValuesList().size() == 1 && conflictClause != null ) {
			// Potentially apply a limit 1 to allow the use of the conflict clause emulation
			querySpec.setFetchClauseExpression(
					new QueryLiteral<>(
							1,
							executionContext.getSession().getFactory().getQueryEngine()
									.getCriteriaBuilder().getIntegerType()
					),
					FetchClauseType.ROWS_ONLY
			);
		}
		final var insertStatement = new InsertSelectStatement( dmlTableReference );
		insertStatement.setConflictClause( conflictClause );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference );
		final JdbcSelect temporaryTableIdentitySelect;
		final JdbcOperationQueryMutation temporaryTableIdUpdate;
		final String temporaryTableRowNumberSelectSql;
		final JdbcOperationQueryMutation rootTableInsert;
		final JdbcOperationQueryMutation temporaryTableIdentityUpdate;
		final String rootTableInsertWithReturningSql;
		final var sqlAstTranslatorFactory =
				getSessionFactory().getJdbcServices()
						.getJdbcEnvironment().getSqlAstTranslatorFactory();
		if ( !assignsId && generator.generatedOnExecution() ) {
			final var identifierMapping =
					(BasicEntityIdentifierMapping)
							getEntityDescriptor().getIdentifierMapping();
			final var idSelectQuerySpec = new QuerySpec( true );
			idSelectQuerySpec.getFromClause().addRoot( temporaryTableGroup );
			final var columnReference = new ColumnReference(
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
				final var sessionUidColumn = entityTable.getSessionUidColumn();
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
			final var selectStatement =
					new SelectStatement(
							idSelectQuerySpec,
							singletonList( new BasicFetch<>(
									0,
									null,
									null,
									identifierMapping,
									FetchTiming.IMMEDIATE,
									false
							) )
					);
			temporaryTableIdentitySelect =
					sqlAstTranslatorFactory.buildSelectTranslator( getSessionFactory(), selectStatement )
							.translate( null, executionContext.getQueryOptions() );
			temporaryTableIdUpdate = null;
			temporaryTableRowNumberSelectSql = null;

			querySpec.applyPredicate(
					new ComparisonPredicate(
							columnReference,
							ComparisonOperator.EQUAL,
							new SqlTypedMappingJdbcParameter( identifierMapping )
					)
			);
		}
		else {
			temporaryTableIdentitySelect = null;
			// if the target paths don't already contain the id, and we need identifier generation,
			// then we load update rows from the temporary table with the generated identifiers,
			// to then insert into the target tables in once statement
			if ( insertStatement.getTargetColumns().stream()
					.noneMatch( c -> keyColumns[0].equals( c.getColumnExpression() ) ) ) {
				final var identifierMapping = getEntityDescriptor().getIdentifierMapping();
				final var primaryKeyTableColumns =
						getPrimaryKeyTableColumns( getEntityDescriptor(), entityTable );

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
				if ( needsIdentifierGeneration( generator, assignsId ) ) {
					final var basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
					final var rootIdentity = new SqlTypedMappingJdbcParameter( basicIdentifierMapping );
					final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
					final var idColumnReference = new ColumnReference( (String) null, basicIdentifierMapping );
					temporaryTableAssignments.add( new Assignment( idColumnReference, rootIdentity ) );

					final int rowNumberIndex =
							entityTable.getColumns().size() - (entityTable.getSessionUidColumn() == null ? 1 : 2);
					final var rowNumberColumn = entityTable.getColumns().get( rowNumberIndex );
					final var rowNumber = new SqlTypedMappingJdbcParameter( rowNumberColumn );

					final var updateStatement = new UpdateStatement(
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

					temporaryTableIdUpdate =
							sqlAstTranslatorFactory.buildMutationTranslator( getSessionFactory(), updateStatement )
									.translate( null, executionContext.getQueryOptions() );
					temporaryTableRowNumberSelectSql =
							createInsertedRowNumbersSelectSql( entityTable, executionContext );
				}
				else {
					temporaryTableIdUpdate = null;
					temporaryTableRowNumberSelectSql = null;
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
			else {
				temporaryTableIdUpdate = null;
				temporaryTableRowNumberSelectSql = null;
			}
		}
		if ( entityTable.getSessionUidColumn() != null ) {
			final var sessionUidColumn = entityTable.getSessionUidColumn();
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

		rootTableInsert =
				sqlAstTranslatorFactory.buildMutationTranslator( getSessionFactory(), insertStatement )
						.translate( null, executionContext.getQueryOptions() );

		if ( !assignsId && generator.generatedOnExecution() ) {
			final var insertDelegate = getEntityDescriptor().getInsertDelegate();
			// todo 7.0 : InsertGeneratedIdentifierDelegate will be removed once we're going to handle
			//            generated values within the jdbc insert operaetion itself
			final var identifierDelegate = (InsertGeneratedIdentifierDelegate) insertDelegate;
			rootTableInsertWithReturningSql = identifierDelegate.prepareIdentifierGeneratingInsert( rootTableInsert.getSqlString() );
			final var identifierMapping =
					(BasicEntityIdentifierMapping)
							getEntityDescriptor().getIdentifierMapping();

			final var primaryKeyTableColumns =
					getPrimaryKeyTableColumns( getEntityDescriptor(), entityTable );
			assert primaryKeyTableColumns.size() == 1;

			final var entityIdentity = new SqlTypedMappingJdbcParameter( identifierMapping );
			final var rootIdentity = new SqlTypedMappingJdbcParameter( identifierMapping );
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
			final var updateStatement = new UpdateStatement(
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

			temporaryTableIdentityUpdate =
					sqlAstTranslatorFactory.buildMutationTranslator( getSessionFactory(), updateStatement )
							.translate( null, executionContext.getQueryOptions() );
		}
		else {
			rootTableInsertWithReturningSql = null;
			temporaryTableIdentityUpdate = null;
		}
		return new RootTableInserter(
				temporaryTableIdentitySelect,
				temporaryTableIdUpdate,
				temporaryTableRowNumberSelectSql,
				rootTableInsert,
				rootTableInsertWithReturningSql,
				temporaryTableIdentityUpdate
		);
	}

	private boolean needsIdentifierGeneration(Generator identifierGenerator, boolean assignsId) {
		if ( !assignsId && identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
			// If the generator uses an optimizer or is not bulk insertion capable,
			// we have to generate identifiers for the new rows, as that couldn't
			// have been done via a SQL expression
			final var optimizer = optimizableGenerator.getOptimizer();
			return optimizer != null && optimizer.getIncrementSize() > 1
				|| identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapable
						&& !bulkInsertionCapable.supportsBulkInsertionIdentifierGeneration();
		}
		else {
			return false;
		}
	}

	private JdbcOperationQueryMutation createTableInsert(
			InsertSelectStatement translatedInsertStatement,
			TableGroup updatingTableGroup,
			String tableExpression,
			String[] keyColumns,
			boolean nullableTable,
			Map<String, List<Assignment>> assignmentsByTable,
			ExecutionContext executionContext) {
		final var updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final var assignments = assignmentsByTable.get( tableExpression );
		if ( nullableTable && ( assignments == null || assignments.isEmpty() ) ) {
			// no assignments for this table - skip it
			return null;
		}
		final var dmlTargetTableReference =
				resolveUnionTableReference( updatingTableReference, tableExpression );

		final var querySpec = new QuerySpec( true );
		final var temporaryTableReference = new NamedTableReference(
				translatedInsertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final var temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				getEntityDescriptor()
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		final var insertStatement = new InsertSelectStatement( dmlTargetTableReference );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference );
		if ( insertStatement.getTargetColumns().stream()
				.noneMatch( column -> keyColumns[0].equals( column.getColumnExpression() ) ) ) {
			final var primaryKeyTableColumns =
					getPrimaryKeyTableColumns( getEntityDescriptor().getEntityPersister(), entityTable );
			getEntityDescriptor().getIdentifierMapping().forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
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
			final var sessionUidColumn = entityTable.getSessionUidColumn();
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
		return getSessionFactory().getJdbcServices()
				.getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( getSessionFactory(), insertStatement )
				.translate( null, executionContext.getQueryOptions() );
	}

	private List<TemporaryTableColumn> getPrimaryKeyTableColumns(EntityPersister entityPersister, TemporaryTable entityTable) {
		final boolean identityColumn = entityPersister.getGenerator().generatedOnExecution();
		final int startIndex = identityColumn ? 1 : 0;
		final int endIndex = startIndex + entityPersister.getIdentifierMapping().getJdbcTypeCount();
		return entityTable.getColumns().subList( startIndex, endIndex );
	}

	private void applyAssignments(
			List<Assignment> assignments,
			InsertSelectStatement insertStatement,
			NamedTableReference temporaryTableReference) {
		if ( assignments != null && !assignments.isEmpty() ) {
			for ( var assignment : assignments ) {
				final var assignable = assignment.getAssignable();
				insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
				final var columns = entityTable.findTemporaryTableColumns(
						getEntityDescriptor().getEntityPersister(),
						( (SqmPathInterpretation<?>) assignable ).getExpressionType()
				);
				for ( var temporaryTableColumn : columns ) {
					insertStatement.getSourceSelectStatement()
							.getFirstQuerySpec().getSelectClause()
							.addSqlSelection( new SqlSelectionImpl(
									new ColumnReference(
											temporaryTableReference.getIdentificationVariable(),
											temporaryTableColumn.getColumnName(),
											false,
											null,
											temporaryTableColumn.getJdbcMapping()
									)
							) );
				}
			}
		}
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding( sessionUidParameter,
					new JdbcParameterBindingImpl( entityTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) ) ) );
		}
		return jdbcParameterBindings;
	}

	@Override
	public boolean dependsOnParameterBindings() {
		if ( temporaryTableInsert.jdbcOperation().dependsOnParameterBindings() ) {
			return true;
		}
		if ( rootTableInserter.temporaryTableIdentitySelect != null
			&& rootTableInserter.temporaryTableIdentitySelect.dependsOnParameterBindings() ) {
			return true;
		}
		if ( rootTableInserter.temporaryTableIdUpdate != null
			&& rootTableInserter.temporaryTableIdUpdate.dependsOnParameterBindings() ) {
			return true;
		}
		if ( rootTableInserter.rootTableInsert.dependsOnParameterBindings() ) {
			return true;
		}
		if ( rootTableInserter.temporaryTableIdentityUpdate != null
			&& rootTableInserter.temporaryTableIdentityUpdate.dependsOnParameterBindings() ) {
			return true;
		}
		for ( var delete : nonRootTableInserts ) {
			if ( delete.dependsOnParameterBindings() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( !temporaryTableInsert.jdbcOperation().isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		if ( rootTableInserter.temporaryTableIdentitySelect != null
			&& rootTableInserter.temporaryTableIdentitySelect.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return true;
		}
		if ( rootTableInserter.temporaryTableIdUpdate != null
			&& rootTableInserter.temporaryTableIdUpdate.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return true;
		}
		if ( rootTableInserter.rootTableInsert.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return true;
		}
		if ( rootTableInserter.temporaryTableIdentityUpdate != null
			&& rootTableInserter.temporaryTableIdentityUpdate.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return true;
		}
		for ( var delete : nonRootTableInserts ) {
			if ( !delete.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
				return false;
			}
		}
		return true;
	}

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		if ( columnReference.getQualifier() == null ) {
			// This happens only for the special row_number column
			return null;
		}
		final var tableReferenceByQualifier =
				tableReferenceByAlias.get( columnReference.getQualifier() );
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

	public SqmInsertStatement<?> getSqmInsertStatement() {
		return (SqmInsertStatement<?>) getSqmStatement();
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext context) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Starting multi-table insert execution - "
					+ getSqmStatement().getTarget().getModel().getName() );
		}

		final var executionContext = omittingLockingAndPaging( context );
		// NOTE: we could get rid of using a temporary table if the expressions in Values are "stable".
		// But that is a non-trivial optimization that requires more effort
		// as we need to split out individual inserts if we have a non-bulk capable optimizer
		final boolean createdTable =
				performBeforeTemporaryTableUseActions(
						entityTable,
						temporaryTableStrategy,
						executionContext
				);
		try {
			final int rows =
					saveIntoTemporaryTable( temporaryTableInsert.jdbcOperation(),
							jdbcParameterBindings, executionContext );
			if ( rows != 0 ) {
				final var sessionUidBindings = new JdbcParameterBindingsImpl( 1 );
				if ( sessionUidParameter != null ) {
					sessionUidBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) )
							)
					);
				}

				final int insertedRows =
						insertRootTable(
								rows,
								createdTable,
								sessionUidBindings,
								executionContext
						);

				for ( var nonRootTableInsert : nonRootTableInserts ) {
					insertTable(
							nonRootTableInsert,
							sessionUidBindings,
							executionContext
					);
				}
				return insertedRows;
			}

			return rows;
		}
		finally {
			performAfterTemporaryTableUseActions(
					entityTable,
					sessionUidAccess,
					getAfterUseAction(),
					executionContext
			);
		}
	}

	private int insertRootTable(
			int rows,
			boolean rowNumberStartsAtOne,
			JdbcParameterBindings sessionUidBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		final var entityPersister = getEntityDescriptor().getEntityPersister();
		final var generator = entityPersister.getGenerator();
		final var identifierMapping = entityPersister.getIdentifierMapping();

		final var session = executionContext.getSession();
		final var jdbcServices = session.getFactory().getJdbcServices();

		final Map<Object, Object> entityTableToRootIdentity;
		if ( rootTableInserter.temporaryTableIdentitySelect != null ) {
			final var list = jdbcServices.getJdbcSelectExecutor().list(
					rootTableInserter.temporaryTableIdentitySelect,
					sessionUidBindings,
					executionContext,
					null,
					null,
					ListResultsConsumer.UniqueSemantic.NONE,
					rows
			);
			entityTableToRootIdentity = new LinkedHashMap<>( list.size() );
			for ( Object object : list ) {
				entityTableToRootIdentity.put( object, null );
			}
		}
		else {
			entityTableToRootIdentity = null;

			if ( rootTableInserter.temporaryTableIdUpdate != null ) {
				final var beforeExecutionGenerator = (BeforeExecutionGenerator) generator;
				final var rowNumberStream = rowNumberStream( rows, rowNumberStartsAtOne, executionContext );
				final var updateBindings = new JdbcParameterBindingsImpl( 3 );
				if ( sessionUidParameter != null ) {
					updateBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( session ) )
							)
					);
				}
				final var parameterBinders = rootTableInserter.temporaryTableIdUpdate.getParameterBinders();
				final var rootIdentity = (JdbcParameter) parameterBinders.get( 0 );
				final var rowNumber = (JdbcParameter) parameterBinders.get( 1 );
				final var basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
				rowNumberStream.forEach( rowNumberValue -> {
					updateBindings.addBinding(
							rowNumber,
							new JdbcParameterBindingImpl(
									rowNumber.getExpressionType().getSingleJdbcMapping(),
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
							rootTableInserter.temporaryTableIdUpdate,
							updateBindings,
							sql -> session.getJdbcCoordinator().getStatementPreparer()
									.prepareStatement( sql ),
							(integer, preparedStatement) -> {},
							executionContext
					);
					assert updateCount == 1;
				} );
			}
		}

		if ( rootTableInserter.rootTableInsertWithReturningSql != null ) {
			final var insertDelegate = entityPersister.getEntityPersister().getInsertDelegate();
			final var basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
			// todo 7.0 : InsertGeneratedIdentifierDelegate will be removed once we're going to handle
			//            generated values within the jdbc insert operaetion itself
			final var identifierDelegate = (InsertGeneratedIdentifierDelegate) insertDelegate;
			final var jdbcValueBinder = basicIdentifierMapping.getJdbcMapping().getJdbcValueBinder();
			for ( var entry : entityTableToRootIdentity.entrySet() ) {
				final var generatedValues = identifierDelegate.performInsertReturning(
						rootTableInserter.rootTableInsertWithReturningSql,
						session,
						new Binder() {
							@Override
							public void bindValues(PreparedStatement ps) throws SQLException {
								jdbcValueBinder.bind( ps, entry.getKey(), 1, session );
								if ( sessionUidParameter != null ) {
									sessionUidParameter.getParameterBinder().bindParameterValue(
											ps,
											2,
											sessionUidBindings,
											executionContext
									);
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
			final var updateBindings = new JdbcParameterBindingsImpl( 2 );
			final var parameterBinders = rootTableInserter.temporaryTableIdentityUpdate.getParameterBinders();
			final var rootIdentity = (JdbcParameter) parameterBinders.get( 0 );
			final var entityIdentity = (JdbcParameter) parameterBinders.get( 1 );
			for ( var entry : entityTableToRootIdentity.entrySet() ) {
				final var jdbcMapping = basicIdentifierMapping.getJdbcMapping();
				updateBindings.addBinding( entityIdentity,
						new JdbcParameterBindingImpl( jdbcMapping, entry.getKey() ) );
				updateBindings.addBinding( rootIdentity,
						new JdbcParameterBindingImpl( jdbcMapping, entry.getValue() ) );
				jdbcServices.getJdbcMutationExecutor().execute(
						rootTableInserter.temporaryTableIdentityUpdate,
						updateBindings,
						sql -> session.getJdbcCoordinator().getStatementPreparer()
								.prepareStatement( sql ),
						(integer, preparedStatement) -> {},
						executionContext
				);
			}
			return entityTableToRootIdentity.size();
		}
		else {
			return jdbcServices.getJdbcMutationExecutor().execute(
					rootTableInserter.rootTableInsert,
					sessionUidBindings,
					sql -> session.getJdbcCoordinator().getStatementPreparer()
							.prepareStatement( sql ),
					(integer, preparedStatement) -> {},
					executionContext
			);
		}
	}

	private IntStream rowNumberStream(
			int rows, boolean rowNumberStartsAtOne,
			SqmJdbcExecutionContextAdapter executionContext) {
		return !rowNumberStartsAtOne
				? IntStream.of( loadInsertedRowNumbers(
					rootTableInserter.temporaryTableRowNumberSelectSql,
					entityTable, sessionUidAccess, rows, executionContext
				) )
				: IntStream.range( 1, rows + 1 );
	}

	private void insertTable(
			JdbcOperationQueryMutation nonRootTableInsert,
			JdbcParameterBindings sessionUidBindings,
			ExecutionContext executionContext) {
		executionContext.getSession().getFactory().getJdbcServices()
				.getJdbcMutationExecutor().execute(
						nonRootTableInsert,
						sessionUidBindings,
						sql -> executionContext.getSession()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql ),
						(integer, preparedStatement) -> {},
						executionContext
				);
	}

	// For Hibernate Reactive
	protected CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> getTemporaryTableInsert() {
		return temporaryTableInsert;
	}

	// For Hibernate Reactive
	protected RootTableInserter getRootTableInserter() {
		return rootTableInserter;
	}

	// For Hibernate Reactive
	protected List<JdbcOperationQueryMutation> getNonRootTableInserts() {
		return nonRootTableInserts;
	}

	// For Hibernate Reactive
	protected TemporaryTable getEntityTable() {
		return entityTable;
	}

	// For Hibernate Reactive
	protected TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	// For Hibernate Reactive
	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	// For Hibernate Reactive
	protected AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	// For Hibernate Reactive
	protected @Nullable JdbcParameter getSessionUidParameter() {
		return sessionUidParameter;
	}
}
