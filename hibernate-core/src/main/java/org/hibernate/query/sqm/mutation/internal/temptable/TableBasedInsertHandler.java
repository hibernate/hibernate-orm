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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.dialect.temptable.TemporaryTableSessionUidColumn;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.insert.Binder;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.internal.util.MutableObject;
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
import org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
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
import org.hibernate.sql.ast.tree.select.QueryPart;
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
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.BasicType;

import org.hibernate.type.descriptor.ValueBinder;
import org.jboss.logging.Logger;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper.isId;

/**
* @author Christian Beikov
*/
public class TableBasedInsertHandler extends AbstractMutationHandler implements InsertHandler {
	private static final Logger log = Logger.getLogger( TableBasedInsertHandler.class );

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

		final TemporaryTableSessionUidColumn sessionUidColumn = entityTable.getSessionUidColumn();
		if ( sessionUidColumn == null ) {
			this.sessionUidParameter = null;
		}
		else {
			this.sessionUidParameter = new JdbcParameterImpl( sessionUidColumn.getJdbcMapping() );
		}
		final SqmJdbcExecutionContextAdapter executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		final MultiTableSqmMutationConverter sqmConverter = new MultiTableSqmMutationConverter(
				getEntityDescriptor(),
				sqmInsert,
				sqmInsert.getTarget(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				getSessionFactory().getSqlTranslationEngine()
		);

		final TableGroup insertingTableGroup = sqmConverter.getMutatingTableGroup();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the insertion target using our special converter, collecting
		// information about the target paths

		final List<Assignment> targetPathColumns = new ArrayList<>();
		final NamedTableReference entityTableReference = new NamedTableReference(
				entityTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true
		);
		final InsertSelectStatement insertStatement = new InsertSelectStatement( entityTableReference );

		final BaseSqmToSqlAstConverter.AdditionalInsertValues additionalInsertValues = sqmConverter.visitInsertionTargetPaths(
				(assignable, columnReferences) -> {
					final SqmPathInterpretation<?> pathInterpretation = (SqmPathInterpretation<?>) assignable;
					final List<TemporaryTableColumn> columns =
							entityTable.findTemporaryTableColumns( getEntityDescriptor(), pathInterpretation.getExpressionType() );
					for ( TemporaryTableColumn column : columns ) {
						insertStatement.addTargetColumnReference( new ColumnReference(
								entityTableReference,
								column.getColumnName(),
								column.getJdbcMapping()
						) );
					}
					targetPathColumns.add( new Assignment( assignable, (Expression) assignable ) );
				},
				sqmInsert,
				getEntityDescriptor(),
				insertingTableGroup
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		if ( sqmInsert instanceof SqmInsertSelectStatement ) {
			final QueryPart queryPart = sqmConverter.visitQueryPart( ( (SqmInsertSelectStatement<?>) sqmInsert ).getSelectQueryPart() );
			queryPart.visitQuerySpecs(
					querySpec -> {
						if ( additionalInsertValues.applySelections( querySpec, getSessionFactory() ) ) {
							final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
							final ColumnReference columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns().set(
									insertStatement.getTargetColumns().size() - 1,
									columnReference
							);
							targetPathColumns.set(
									targetPathColumns.size() - 1,
									new Assignment( columnReference, columnReference )
							);
						}
						else if ( !(getEntityDescriptor().getGenerator() instanceof OnExecutionGenerator generator
									&& generator.generatedOnExecution())
								&& !entityTable.isRowNumberGenerated() ) {
							final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
									.get( entityTable.getColumns().size() - ( sessionUidColumn == null ? 1 : 2 ) );
							final ColumnReference columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnName(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns().add( columnReference );
							targetPathColumns.add( new Assignment( columnReference, columnReference ) );
							querySpec.getSelectClause().addSqlSelection(
									new SqlSelectionImpl(
											0,
											SqmInsertStrategyHelper.createRowNumberingExpression(
													querySpec,
													getSessionFactory()
											)
									)
							);
						}
						if ( sessionUidColumn != null ) {
							final ColumnReference sessionUidColumnReference = new ColumnReference(
									(String) null,
									sessionUidColumn.getColumnName(),
									false,
									null,
									sessionUidColumn.getJdbcMapping()
							);
							querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl(
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
			if ( !(getEntityDescriptor().getGenerator() instanceof OnExecutionGenerator generator
				&& generator.generatedOnExecution())
				&& !entityTable.isRowNumberGenerated() ) {
				final TemporaryTableColumn rowNumberColumn = entityTable.getColumns()
						.get( entityTable.getColumns().size() - (sessionUidColumn == null ? 1 : 2) );
				rowNumberType = (BasicType<?>) rowNumberColumn.getJdbcMapping();
				final ColumnReference columnReference = new ColumnReference(
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
				final ColumnReference sessionUidColumnReference = new ColumnReference(
						(String) null,
						sessionUidColumn.getColumnName(),
						false,
						null,
						sessionUidColumn.getJdbcMapping()
				);
				insertStatement.getTargetColumns().add( sessionUidColumnReference );
				targetPathColumns.add( new Assignment( sessionUidColumnReference, sessionUidParameter ) );
			}
			final List<SqmValues> sqmValuesList = ( (SqmInsertValuesStatement<?>) sqmInsert ).getValuesList();
			final List<Values> valuesList = new ArrayList<>( sqmValuesList.size() );
			for ( int i = 0; i < sqmValuesList.size(); i++ ) {
				final Values values = sqmConverter.visitValues( sqmValuesList.get( i ) );
				additionalInsertValues.applyValues( values );
				if ( rowNumberType != null ) {
					values.getExpressions().add(
							new QueryLiteral<>(
									rowNumberType.getJavaTypeDescriptor()
											.wrap( i + 1, getSessionFactory().getWrapperOptions() ),
									rowNumberType
							)
					);
				}
				if ( sessionUidParameter != null ) {
					values.getExpressions().add( sessionUidParameter );
				}
				valuesList.add( values );
			}
			insertStatement.setValuesList( valuesList );
		}
		final ConflictClause conflictClause = sqmConverter.visitConflictClause( sqmInsert.getConflictClause() );
		sqmConverter.pruneTableGroupJoins();

		boolean assignsId = false;
		for ( Assignment assignment : targetPathColumns ) {
			assignsId = assignsId || assignment.getAssignable() instanceof SqmPathInterpretation<?> pathInterpretation
									&& isId( pathInterpretation.getExpressionType() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( insertingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < insertingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( insertingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final ModelPartContainer updatingModelPart = insertingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		final Map<String, List<Assignment>> assignmentsByTable =
				CollectionHelper.mapOfSize( insertingTableGroup.getTableReferenceJoins().size() + 1 );

		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter );
		this.resolvedParameterMappingModelTypes = sqmConverter.getSqmParameterMappingModelExpressibleResolutions();

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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
			final Assignment assignment = targetPathColumns.get( i );
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

		this.temporaryTableInsert = ExecuteWithTemporaryTableHelper.createTemporaryTableInsert(
				insertStatement,
				jdbcParameterBindings,
				executionContext
		);

		final int tableSpan = getEntityDescriptor().getTableSpan();
		this.rootTableInserter = createRootTableInserter(
				insertStatement,
				insertingTableGroup,
				conflictClause,
				assignsId,
				getEntityDescriptor().getTableName( 0 ),
				getEntityDescriptor().getKeyColumns( 0 ),
				assignmentsByTable,
				executionContext
		);

		final ArrayList<JdbcOperationQueryMutation> nonRootTableInserts = new ArrayList<>( tableSpan );
		if ( getEntityDescriptor().hasDuplicateTables() ) {
			final String[] insertedTables = new String[tableSpan];
			insertedTables[0] = getEntityDescriptor().getTableName( 0 );
			for ( int i = 1; i < tableSpan; i++ ) {
				if ( getEntityDescriptor().isInverseTable( i ) ) {
					continue;
				}
				final String tableName = getEntityDescriptor().getTableName( i );
				insertedTables[i] = tableName;
				if ( ArrayHelper.indexOf( insertedTables, i, tableName ) != -1 ) {
					// Since secondary tables could appear multiple times, we have to skip duplicates
					continue;
				}
				final JdbcOperationQueryMutation insert = createTableInsert(
						insertStatement,
						insertingTableGroup,
						tableName,
						getEntityDescriptor().getKeyColumns( i ),
						getEntityDescriptor().isNullableTable( i ),
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
				final JdbcOperationQueryMutation insert = createTableInsert(
						insertStatement,
						insertingTableGroup,
						getEntityDescriptor().getTableName( i ),
						getEntityDescriptor().getKeyColumns( i ),
						getEntityDescriptor().isNullableTable( i ),
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
			@Nullable JdbcOperationQuerySelect temporaryTableIdentitySelect,
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
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final Generator generator = getEntityDescriptor().getGenerator();
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
				translatedInsertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
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
							executionContext.getSession().getFactory().getQueryEngine().getCriteriaBuilder().getIntegerType()
					),
					FetchClauseType.ROWS_ONLY
			);
		}
		final InsertSelectStatement insertStatement = new InsertSelectStatement( dmlTableReference );
		insertStatement.setConflictClause( conflictClause );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference, getEntityDescriptor() );
		final JdbcServices jdbcServices = getSessionFactory().getJdbcServices();
		final SharedSessionContractImplementor session = executionContext.getSession();
		final JdbcOperationQuerySelect temporaryTableIdentitySelect;
		final JdbcOperationQueryMutation temporaryTableIdUpdate;
		final String temporaryTableRowNumberSelectSql;
		final JdbcOperationQueryMutation rootTableInsert;
		final JdbcOperationQueryMutation temporaryTableIdentityUpdate;
		final String rootTableInsertWithReturningSql;
		if ( !assignsId && generator.generatedOnExecution() ) {
			final BasicEntityIdentifierMapping identifierMapping =
					(BasicEntityIdentifierMapping) getEntityDescriptor().getIdentifierMapping();
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
			temporaryTableIdentitySelect = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildSelectTranslator( getSessionFactory(), selectStatement )
					.translate( null, executionContext.getQueryOptions() );
			temporaryTableIdUpdate = null;
			temporaryTableRowNumberSelectSql = null;

			querySpec.applyPredicate(
					new ComparisonPredicate(
							columnReference,
							ComparisonOperator.EQUAL,
							new JdbcParameterImpl( identifierMapping.getJdbcMapping() )
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
				final EntityIdentifierMapping identifierMapping = getEntityDescriptor().getIdentifierMapping();
				final List<TemporaryTableColumn> primaryKeyTableColumns =
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
					final BasicEntityIdentifierMapping basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
					final JdbcParameter rootIdentity = new JdbcParameterImpl( basicIdentifierMapping.getJdbcMapping() );
					final List<Assignment> temporaryTableAssignments = new ArrayList<>( 1 );
					final ColumnReference idColumnReference = new ColumnReference( (String) null, basicIdentifierMapping );
					temporaryTableAssignments.add( new Assignment( idColumnReference, rootIdentity ) );

					final int rowNumberIndex =
							entityTable.getColumns().size() - (entityTable.getSessionUidColumn() == null ? 1 : 2);
					final TemporaryTableColumn rowNumberColumn = entityTable.getColumns().get( rowNumberIndex );
					final JdbcParameter rowNumber = new JdbcParameterImpl( rowNumberColumn.getJdbcMapping() );

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

					temporaryTableIdUpdate = jdbcServices.getJdbcEnvironment()
							.getSqlAstTranslatorFactory()
							.buildMutationTranslator( getSessionFactory(), updateStatement )
							.translate( null, executionContext.getQueryOptions() );
					temporaryTableRowNumberSelectSql = ExecuteWithTemporaryTableHelper.createInsertedRowNumbersSelectSql(
							entityTable,
							sessionUidAccess,
							executionContext
					);
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

		rootTableInsert = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( getSessionFactory(), insertStatement )
				.translate( null, executionContext.getQueryOptions() );

		if ( !assignsId && generator.generatedOnExecution() ) {
			final GeneratedValuesMutationDelegate insertDelegate = getEntityDescriptor().getInsertDelegate();
			// todo 7.0 : InsertGeneratedIdentifierDelegate will be removed once we're going to handle
			//            generated values within the jdbc insert operaetion itself
			final InsertGeneratedIdentifierDelegate identifierDelegate = (InsertGeneratedIdentifierDelegate) insertDelegate;
			rootTableInsertWithReturningSql = identifierDelegate.prepareIdentifierGeneratingInsert( rootTableInsert.getSqlString() );
			final BasicEntityIdentifierMapping identifierMapping =
					(BasicEntityIdentifierMapping) getEntityDescriptor().getIdentifierMapping();

			final List<TemporaryTableColumn> primaryKeyTableColumns =
					getPrimaryKeyTableColumns( getEntityDescriptor(), entityTable );
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

			temporaryTableIdentityUpdate = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildMutationTranslator( getSessionFactory(), updateStatement )
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

	private JdbcOperationQueryMutation createTableInsert(
			InsertSelectStatement translatedInsertStatement,
			TableGroup updatingTableGroup,
			String tableExpression,
			String[] keyColumns,
			boolean nullableTable,
			Map<String, List<Assignment>> assignmentsByTable,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final List<Assignment> assignments = assignmentsByTable.get( tableExpression );
		if ( nullableTable && ( assignments == null || assignments.isEmpty() ) ) {
			// no assignments for this table - skip it
			return null;
		}
		final NamedTableReference dmlTargetTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );

		final QuerySpec querySpec = new QuerySpec( true );
		final NamedTableReference temporaryTableReference = new NamedTableReference(
				translatedInsertStatement.getTargetTable().getTableExpression(),
				"hte_tmp"
		);
		final TableGroupImpl temporaryTableGroup = new TableGroupImpl(
				updatingTableGroup.getNavigablePath(),
				null,
				temporaryTableReference,
				getEntityDescriptor()
		);
		querySpec.getFromClause().addRoot( temporaryTableGroup );
		final InsertSelectStatement insertStatement = new InsertSelectStatement( dmlTargetTableReference );
		insertStatement.setSourceSelectStatement( querySpec );
		applyAssignments( assignments, insertStatement, temporaryTableReference, getEntityDescriptor() );
		if ( insertStatement.getTargetColumns()
				.stream()
				.noneMatch( c -> keyColumns[0].equals( c.getColumnExpression() ) ) ) {
			final List<TemporaryTableColumn> primaryKeyTableColumns =
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
		final JdbcServices jdbcServices = getSessionFactory().getJdbcServices();
		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( getSessionFactory(), insertStatement )
				.translate( null, executionContext.getQueryOptions() );
	}

	private List<TemporaryTableColumn> getPrimaryKeyTableColumns(EntityPersister entityPersister, TemporaryTable entityTable) {
		final boolean identityColumn = entityPersister.getGenerator().generatedOnExecution();
		final int startIndex = identityColumn ? 1 : 0;
		final int endIndex = startIndex + entityPersister.getIdentifierMapping().getJdbcTypeCount();
		return entityTable.getColumns().subList( startIndex, endIndex );
	}

	private void applyAssignments(List<Assignment> assignments, InsertSelectStatement insertStatement, NamedTableReference temporaryTableReference, EntityPersister entityDescriptor) {
		if ( assignments != null && !assignments.isEmpty() ) {
			for ( Assignment assignment : assignments ) {
				final Assignable assignable = assignment.getAssignable();
				insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
				final List<TemporaryTableColumn> columns = entityTable.findTemporaryTableColumns(
						getEntityDescriptor().getEntityPersister(),
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

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							entityTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
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
		for ( JdbcOperationQueryMutation delete : nonRootTableInserts ) {
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
		for ( JdbcOperationQueryMutation delete : nonRootTableInserts ) {
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

	public SqmInsertStatement<?> getSqmInsertStatement() {
		return (SqmInsertStatement<?>) getSqmStatement();
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext context) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table insert execution - %s",
					getSqmStatement().getTarget().getModel().getName()
			);
		}

		final SqmJdbcExecutionContextAdapter executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		// NOTE: we could get rid of using a temporary table if the expressions in Values are "stable".
		// But that is a non-trivial optimization that requires more effort
		// as we need to split out individual inserts if we have a non-bulk capable optimizer
		final boolean createdTable = ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				entityTable,
				temporaryTableStrategy,
				executionContext
		);

		try {
			final int rows = ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable(
					temporaryTableInsert.jdbcOperation(),
					jdbcParameterBindings,
					executionContext
			);

			if ( rows != 0 ) {
				final JdbcParameterBindings sessionUidBindings = new JdbcParameterBindingsImpl( 1 );
				if ( sessionUidParameter != null ) {
					sessionUidBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) )
							)
					);
				}

				final int insertedRows = insertRootTable(
						rows,
						createdTable,
						sessionUidBindings,
						executionContext
				);

				for ( JdbcOperationQueryMutation nonRootTableInsert : nonRootTableInserts ) {
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
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
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
		final EntityPersister entityPersister = getEntityDescriptor().getEntityPersister();
		final Generator generator = entityPersister.getGenerator();
		final EntityIdentifierMapping identifierMapping = entityPersister.getIdentifierMapping();

		final SharedSessionContractImplementor session = executionContext.getSession();
		final JdbcServices jdbcServices = session.getFactory().getJdbcServices();

		final Map<Object, Object> entityTableToRootIdentity;
		if ( rootTableInserter.temporaryTableIdentitySelect != null ) {
			final List<Object> list = jdbcServices.getJdbcSelectExecutor().list(
					rootTableInserter.temporaryTableIdentitySelect,
					sessionUidBindings,
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
		}
		else {
			entityTableToRootIdentity = null;

			if ( rootTableInserter.temporaryTableIdUpdate != null ) {
				final BeforeExecutionGenerator beforeExecutionGenerator = (BeforeExecutionGenerator) generator;
				final IntStream rowNumberStream = !rowNumberStartsAtOne
						? IntStream.of( ExecuteWithTemporaryTableHelper.loadInsertedRowNumbers(
						rootTableInserter.temporaryTableRowNumberSelectSql, entityTable, sessionUidAccess, rows,
						executionContext ) )
						: IntStream.range( 1, rows + 1 );
				final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 3 );
				if ( sessionUidParameter != null ) {
					updateBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( session ) )
							)
					);
				}
				final List<JdbcParameterBinder> parameterBinders = rootTableInserter.temporaryTableIdUpdate.getParameterBinders();
				final JdbcParameter rootIdentity = (JdbcParameter) parameterBinders.get( 0 );
				final JdbcParameter rowNumber = (JdbcParameter) parameterBinders.get( 1 );
				final BasicEntityIdentifierMapping basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
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
		}

		if ( rootTableInserter.rootTableInsertWithReturningSql != null ) {
			final GeneratedValuesMutationDelegate insertDelegate = entityPersister.getEntityPersister().getInsertDelegate();
			final BasicEntityIdentifierMapping basicIdentifierMapping = (BasicEntityIdentifierMapping) identifierMapping;
			// todo 7.0 : InsertGeneratedIdentifierDelegate will be removed once we're going to handle
			//            generated values within the jdbc insert operaetion itself
			final InsertGeneratedIdentifierDelegate identifierDelegate = (InsertGeneratedIdentifierDelegate) insertDelegate;
			final ValueBinder jdbcValueBinder = basicIdentifierMapping.getJdbcMapping().getJdbcValueBinder();
			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				final GeneratedValues generatedValues = identifierDelegate.performInsertReturning(
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
			final JdbcParameterBindings updateBindings = new JdbcParameterBindingsImpl( 2 );

			final List<JdbcParameterBinder> parameterBinders = rootTableInserter.temporaryTableIdentityUpdate.getParameterBinders();
			final JdbcParameter rootIdentity = (JdbcParameter) parameterBinders.get( 0 );
			final JdbcParameter entityIdentity = (JdbcParameter) parameterBinders.get( 1 );
			for ( Map.Entry<Object, Object> entry : entityTableToRootIdentity.entrySet() ) {
				JdbcMapping jdbcMapping = basicIdentifierMapping.getJdbcMapping();
				updateBindings.addBinding( entityIdentity, new JdbcParameterBindingImpl( jdbcMapping, entry.getKey() ) );
				updateBindings.addBinding( rootIdentity, new JdbcParameterBindingImpl( jdbcMapping, entry.getValue() ) );
				jdbcServices.getJdbcMutationExecutor().execute(
						rootTableInserter.temporaryTableIdentityUpdate,
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
					rootTableInserter.rootTableInsert,
					sessionUidBindings,
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

	private void insertTable(
			JdbcOperationQueryMutation nonRootTableInsert,
			JdbcParameterBindings sessionUidBindings,
			ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final JdbcServices jdbcServices = session.getFactory().getJdbcServices();
		jdbcServices.getJdbcMutationExecutor().execute(
				nonRootTableInsert,
				sessionUidBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
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
