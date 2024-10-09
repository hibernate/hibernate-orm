/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.InsertHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmConflictClause;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.from.DerivedTableReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableGroup;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.generator.Generator;
import org.hibernate.type.BasicType;

/**
 *
 * @author Christian Beikov
 */
public class CteInsertHandler implements InsertHandler {

	public static final String DML_RESULT_TABLE_NAME_PREFIX = "dml_cte_";
	public static final String CTE_TABLE_IDENTIFIER = "id";
	public static final String ROW_NUMBERS_WITH_SEQUENCE_VALUE = "rows_with_next_val";

	private final SqmInsertStatement<?> sqmStatement;

	private final SessionFactoryImplementor sessionFactory;
	private final EntityMappingType entityDescriptor;
	private final CteTable cteTable;
	private final DomainParameterXref domainParameterXref;

	public CteInsertHandler(
			CteTable cteTable,
			SqmInsertStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			SessionFactoryImplementor sessionFactory) {
		this.sqmStatement = sqmStatement;
		this.sessionFactory = sessionFactory;

		final String entityName = this.sqmStatement.getTarget()
				.getModel()
				.getHibernateEntityName();

		this.entityDescriptor = sessionFactory.getRuntimeMetamodels().getEntityMappingType( entityName );
		this.cteTable = cteTable;
		this.domainParameterXref = domainParameterXref;
	}

	public static CteTable createCteTable(CteTable sqmCteTable, List<CteColumn> sqmCteColumns) {
		return new CteTable( sqmCteTable.getTableExpression(), sqmCteColumns );
	}

	public SqmInsertStatement<?> getSqmStatement() {
		return sqmStatement;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		final SqmInsertStatement<?> sqmInsertStatement = getSqmStatement();
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final EntityPersister entityDescriptor = getEntityDescriptor().getEntityPersister();
		final SqmRoot<?> target = sqmInsertStatement.getTarget();
		final String explicitDmlTargetAlias =
				target.getExplicitAlias() == null
						? "dml_target"
						: target.getExplicitAlias();

		final MultiTableSqmMutationConverter sqmConverter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmInsertStatement,
				target,
				explicitDmlTargetAlias,
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				factory
		);
		final TableGroup insertingTableGroup = sqmConverter.getMutatingTableGroup();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the insertion target using our special converter, collecting
		// information about the target paths

		final int size = sqmStatement.getInsertionTargetPaths().size();
		final List<Map.Entry<List<CteColumn>, Assignment>> targetPathColumns = new ArrayList<>( size );
		final List<CteColumn> targetPathCteColumns = new ArrayList<>( size );

		final BaseSqmToSqlAstConverter.AdditionalInsertValues additionalInsertValues = sqmConverter.visitInsertionTargetPaths(
				(assignable, columnReferences) -> {
					final SqmPathInterpretation<?> pathInterpretation = (SqmPathInterpretation<?>) assignable;
					final int offset = CteTable.determineModelPartStartIndex(
							entityDescriptor,
							pathInterpretation.getExpressionType()
					);
					if ( offset == -1 ) {
						throw new IllegalStateException( "Couldn't find matching cte column for: " + ( (Expression) assignable ).getExpressionType() );
					}
					final int end = offset + pathInterpretation.getExpressionType().getJdbcTypeCount();
					// Find a matching cte table column and set that at the current index
					final List<CteColumn> columns = cteTable.getCteColumns().subList( offset, end );
					targetPathCteColumns.addAll( columns );
					targetPathColumns.add(
							new AbstractMap.SimpleEntry<>(
									columns,
									new Assignment(
											assignable,
											(Expression) assignable
									)
							)
					);
				},
				sqmInsertStatement,
				entityDescriptor,
				insertingTableGroup
		);

		final boolean assignsId = targetPathCteColumns.contains( cteTable.getCteColumns().get( 0 ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the statement that represent the source for the entity cte

		final Stack<SqlAstProcessingState> processingStateStack = sqmConverter.getProcessingStateStack();
		final SqlAstProcessingState oldState = processingStateStack.pop();
		final Statement queryStatement;
		if ( sqmInsertStatement instanceof SqmInsertSelectStatement ) {
			final QueryPart queryPart = sqmConverter.visitQueryPart( ( (SqmInsertSelectStatement<?>) sqmInsertStatement ).getSelectQueryPart() );
			queryPart.visitQuerySpecs(
					querySpec -> {
						// This returns true if the insertion target uses a sequence with an optimizer
						// in which case we will fill the row_number column instead of the id column
						if ( additionalInsertValues.applySelections( querySpec, sessionFactory ) ) {
							final CteColumn rowNumberColumn = cteTable.getCteColumns()
									.get( cteTable.getCteColumns().size() - 1 );
							targetPathCteColumns.set(
									targetPathCteColumns.size() - 1,
									rowNumberColumn
							);
						}
						if ( !assignsId && entityDescriptor.getGenerator().generatedOnExecution() ) {
							querySpec.getSelectClause().addSqlSelection(
									new SqlSelectionImpl(
											0,
											SqmInsertStrategyHelper.createRowNumberingExpression(
													querySpec,
													sessionFactory
											)
									)
							);
						}
					}
			);
			queryStatement = new SelectStatement( queryPart );
		}
		else {
			final List<SqmValues> sqmValuesList = ( (SqmInsertValuesStatement<?>) sqmInsertStatement ).getValuesList();
			final List<Values> valuesList = new ArrayList<>( sqmValuesList.size() );
			for ( SqmValues sqmValues : sqmValuesList ) {
				final Values values = sqmConverter.visitValues( sqmValues );
				additionalInsertValues.applyValues( values );
				valuesList.add( values );
			}
			final QuerySpec querySpec = new QuerySpec( true );
			final NavigablePath navigablePath = new NavigablePath( entityDescriptor.getRootPathName() );
			final List<String> columnNames = new ArrayList<>( targetPathColumns.size() );
			final String valuesAlias = insertingTableGroup.getPrimaryTableReference().getIdentificationVariable();
			for ( Map.Entry<List<CteColumn>, Assignment> entry : targetPathColumns ) {
				for ( ColumnReference columnReference : entry.getValue().getAssignable().getColumnReferences() ) {
					columnNames.add( columnReference.getColumnExpression() );
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									0,
									columnReference.getQualifier().equals( valuesAlias )
											? columnReference
											: new ColumnReference(
													valuesAlias,
													columnReference.getColumnExpression(),
													false,
													null,
													columnReference.getJdbcMapping()
									)
							)
					);
				}
			}
			final ValuesTableGroup valuesTableGroup = new ValuesTableGroup(
					navigablePath,
					entityDescriptor.getEntityPersister(),
					valuesList,
					insertingTableGroup.getPrimaryTableReference().getIdentificationVariable(),
					columnNames,
					true,
					factory
			);
			querySpec.getFromClause().addRoot( valuesTableGroup );
			queryStatement = new SelectStatement( querySpec );
		}
		processingStateStack.push( oldState );
		sqmConverter.pruneTableGroupJoins();

		if ( !assignsId && entityDescriptor.getGenerator().generatedOnExecution() ) {
			// Add the row number to the assignments
			final CteColumn rowNumberColumn = cteTable.getCteColumns()
					.get( cteTable.getCteColumns().size() - 1 );
			targetPathCteColumns.add( rowNumberColumn );
		}

		final CteTable entityCteTable = createCteTable( getCteTable(), targetPathCteColumns );

		// Create the main query spec that will return the count of rows
		final QuerySpec querySpec = new QuerySpec( true, 1 );
		final List<DomainResult<?>> domainResults = new ArrayList<>( 1 );
		final SelectStatement statement = new SelectStatement( querySpec, domainResults );

		final CteStatement entityCte;
		if ( additionalInsertValues.requiresRowNumberIntermediate() ) {
			final CteTable fullEntityCteTable = getCteTable();
			final String baseTableName = "base_" + entityCteTable.getTableExpression();
			final CteStatement baseEntityCte = new CteStatement(
					entityCteTable.withName( baseTableName ),
					queryStatement,
					// The query cte will be reused multiple times
					CteMaterialization.MATERIALIZED
			);
			statement.addCteStatement( baseEntityCte );

			final CteColumn rowNumberColumn = fullEntityCteTable.getCteColumns().get(
					fullEntityCteTable.getCteColumns().size() - 1
			);
			final ColumnReference rowNumberColumnReference = new ColumnReference(
					"e",
					rowNumberColumn.getColumnExpression(),
					false,
					null,
					rowNumberColumn.getJdbcMapping()
			);
			final CteColumn idColumn = fullEntityCteTable.getCteColumns().get( 0 );
			final BasicValuedMapping idType = (BasicValuedMapping) idColumn.getJdbcMapping();
			final Optimizer optimizer = ( (OptimizableGenerator) entityDescriptor.getGenerator() ).getOptimizer();
			final BasicValuedMapping integerType = (BasicValuedMapping) rowNumberColumn.getJdbcMapping();
			final Expression rowNumberMinusOneModuloIncrement = new BinaryArithmeticExpression(
					new BinaryArithmeticExpression(
							rowNumberColumnReference,
							BinaryArithmeticOperator.SUBTRACT,
							new QueryLiteral<>(
									1,
									(BasicValuedMapping) rowNumberColumn.getJdbcMapping()
							),
							integerType
					),
					BinaryArithmeticOperator.MODULO,
					new QueryLiteral<>(
							optimizer.getIncrementSize(),
							integerType
					),
					integerType
			);

			// Create the CTE that fetches a new sequence value for the row numbers that need it
			{
				final QuerySpec rowsWithSequenceQuery = new QuerySpec( true );
				rowsWithSequenceQuery.getFromClause().addRoot(
						new CteTableGroup( new NamedTableReference( baseTableName, "e" ) )
				);
				rowsWithSequenceQuery.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								0,
								rowNumberColumnReference
						)
				);
				final BulkInsertionCapableIdentifierGenerator generator =
						(BulkInsertionCapableIdentifierGenerator) entityDescriptor.getGenerator();
				final String fragment =
						generator.determineBulkInsertionIdentifierGenerationSelectFragment(
								sessionFactory.getSqlStringGenerationContext()
						);
				rowsWithSequenceQuery.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								1,
								new SelfRenderingSqlFragmentExpression( fragment )
						)
				);
				rowsWithSequenceQuery.applyPredicate(
						new ComparisonPredicate(
								rowNumberMinusOneModuloIncrement,
								ComparisonOperator.EQUAL,
								new QueryLiteral<>(
										0,
										integerType
								)
						)
				);
				final CteTable rowsWithSequenceCteTable = new CteTable(
						ROW_NUMBERS_WITH_SEQUENCE_VALUE,
						List.of( rowNumberColumn, idColumn )
				);
				final SelectStatement rowsWithSequenceStatement = new SelectStatement( rowsWithSequenceQuery );
				final CteStatement rowsWithSequenceCte = new CteStatement(
						rowsWithSequenceCteTable,
						rowsWithSequenceStatement,
						// The query cte will be reused multiple times
						CteMaterialization.MATERIALIZED
				);
				statement.addCteStatement( rowsWithSequenceCte );
			}

			// Create the CTE that represents the entity cte
			{
				final QuerySpec entityQuery = new QuerySpec( true );
				final NavigablePath navigablePath = new NavigablePath( baseTableName );
				final TableGroup baseTableGroup = new TableGroupImpl(
						navigablePath,
						null,
						new NamedTableReference( baseTableName, "e" ),
						null
				);
				final TableGroup rowsWithSequenceTableGroup = new CteTableGroup(
						new NamedTableReference(
								ROW_NUMBERS_WITH_SEQUENCE_VALUE,
								"t"
						)
				);
				baseTableGroup.addTableGroupJoin(
						new TableGroupJoin(
								rowsWithSequenceTableGroup.getNavigablePath(),
								SqlAstJoinType.LEFT,
								rowsWithSequenceTableGroup,
								new ComparisonPredicate(
										new BinaryArithmeticExpression(
												rowNumberColumnReference,
												BinaryArithmeticOperator.SUBTRACT,
												rowNumberMinusOneModuloIncrement,
												integerType
										),
										ComparisonOperator.EQUAL,
										new ColumnReference(
												"t",
												rowNumberColumn.getColumnExpression(),
												false,
												null,
												rowNumberColumn.getJdbcMapping()
										)
								)
						)
				);
				entityQuery.getFromClause().addRoot( baseTableGroup );
				entityQuery.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								0,
								new BinaryArithmeticExpression(
										new ColumnReference(
												"t",
												idColumn.getColumnExpression(),
												false,
												null,
												idColumn.getJdbcMapping()
										),
										BinaryArithmeticOperator.ADD,
										new BinaryArithmeticExpression(
												rowNumberColumnReference,
												BinaryArithmeticOperator.SUBTRACT,
												new ColumnReference(
														"t",
														rowNumberColumn.getColumnExpression(),
														false,
														null,
														rowNumberColumn.getJdbcMapping()
												),
												integerType
										),
										idType
								)
						)
				);
				final CteTable finalEntityCteTable;
				if ( targetPathCteColumns.contains( getCteTable().getCteColumns().get( 0 ) ) ) {
					finalEntityCteTable = entityCteTable;
				}
				else {
					targetPathCteColumns.add( 0, getCteTable().getCteColumns().get( 0 ) );
					finalEntityCteTable = createCteTable( getCteTable(), targetPathCteColumns );
				}
				final List<CteColumn> cteColumns = finalEntityCteTable.getCteColumns();
				for ( int i = 1; i < cteColumns.size(); i++ ) {
					final CteColumn cteColumn = cteColumns.get( i );
					entityQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									i,
									new ColumnReference(
											"e",
											cteColumn.getColumnExpression(),
											false,
											null,
											cteColumn.getJdbcMapping()
									)
							)
					);
				}

				final SelectStatement entityStatement = new SelectStatement( entityQuery );
				entityCte = new CteStatement(
						finalEntityCteTable,
						entityStatement,
						// The query cte will be reused multiple times
						CteMaterialization.MATERIALIZED
				);
				statement.addCteStatement( entityCte );
			}
		}
		else if ( !assignsId && entityDescriptor.getGenerator().generatedOnExecution() ) {
			final String baseTableName = "base_" + entityCteTable.getTableExpression();
			final CteStatement baseEntityCte = new CteStatement(
					entityCteTable.withName( baseTableName ),
					queryStatement,
					// The query cte will be reused multiple times
					CteMaterialization.MATERIALIZED
			);
			statement.addCteStatement( baseEntityCte );
			targetPathCteColumns.add( 0, cteTable.getCteColumns().get( 0 ) );
			final CteTable finalEntityCteTable = createCteTable( getCteTable(), targetPathCteColumns );
			final QuerySpec finalQuerySpec = new QuerySpec( true );
			final SelectStatement finalQueryStatement = new SelectStatement( finalQuerySpec );
			entityCte = new CteStatement(
					finalEntityCteTable,
					finalQueryStatement,
					// The query cte will be reused multiple times
					CteMaterialization.MATERIALIZED
			);
		}
		else {
			entityCte = new CteStatement(
					entityCteTable,
					queryStatement,
					// The query cte will be reused multiple times
					CteMaterialization.MATERIALIZED
			);
			statement.addCteStatement( entityCte );
		}

		// Add all CTEs
		final String baseInsertCte = addDmlCtes(
				statement,
				entityCte,
				targetPathColumns,
				assignsId,
				sqmConverter,
				factory
		);

		final Expression count = createCountStar( factory, sqmConverter );
		domainResults.add(
				new BasicResult<>(
						0,
						null,
						( (SqlExpressible) count).getJdbcMapping()
				)
		);
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, count ) );
		querySpec.getFromClause().addRoot(
				new CteTableGroup(
						new NamedTableReference(
								// We want to return the insertion count of the base table
								baseInsertCte,
								CTE_TABLE_IDENTIFIER
						)
				)
		);

		// Execute the statement
		final JdbcServices jdbcServices = factory.getJdbcServices();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, statement );
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter ),
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmConverter.getSqmParameterMappingModelExpressibleResolutions().get( parameter );
					}
				},
				executionContext.getSession()
		);
		final JdbcOperationQuerySelect select = translator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		executionContext.getSession().autoFlushIfRequired( select.getAffectedTableNames() );
		List<Object> list = jdbcServices.getJdbcSelectExecutor().list(
				select,
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext ),
				RowTransformerSingularReturnImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.NONE,
				1
		);
		return ( (Number) list.get( 0 ) ).intValue();
	}

	protected Expression createCountStar(
			SessionFactoryImplementor factory,
			MultiTableSqmMutationConverter sqmConverter) {
		final SqmExpression<?> arg = new SqmStar( factory.getNodeBuilder() );
		return factory.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "count" ).generateSqmExpression(
				arg,
				null,
				factory.getQueryEngine()
		).convertToSqlAst( sqmConverter );
	}

	protected String addDmlCtes(
			CteContainer statement,
			CteStatement queryCte,
			List<Map.Entry<List<CteColumn>, Assignment>> assignments,
			boolean assignsId,
			MultiTableSqmMutationConverter sqmConverter,
			SessionFactoryImplementor factory) {
		final TableGroup updatingTableGroup = sqmConverter.getMutatingTableGroup();
		final EntityMappingType entityDescriptor = getEntityDescriptor();

		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		final String rootEntityName = entityPersister.getRootEntityName();
		final EntityPersister rootEntityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = rootEntityDescriptor.getTableName();
		final TableReference hierarchyRootTableReference = updatingTableGroup.resolveTableReference(
				updatingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, but the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final Map<TableReference, List<Map.Entry<List<CteColumn>, Assignment>>> assignmentsByTable = CollectionHelper.mapOfSize(
				updatingTableGroup.getTableReferenceJoins().size() + 1
		);

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Map.Entry<List<CteColumn>, Assignment> entry = assignments.get( i );
			final Assignment assignment = entry.getValue();
			final List<ColumnReference> assignmentColumnRefs = assignment.getAssignable().getColumnReferences();

			TableReference assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final TableReference tableReference = resolveTableReference( columnReference, tableReferenceByAlias );

				// TODO: this could be fixed by introducing joins to DML statements
				if ( assignmentTableReference != null && !assignmentTableReference.equals( tableReference ) ) {
					throw new IllegalStateException( "Assignment referred to columns from multiple tables" );
				}

				assignmentTableReference = tableReference;
			}
			assert assignmentTableReference != null;

			List<Map.Entry<List<CteColumn>, Assignment>> assignmentsForTable = assignmentsByTable.get( assignmentTableReference );
			if ( assignmentsForTable == null ) {
				assignmentsForTable = new ArrayList<>();
				assignmentsByTable.put( assignmentTableReference, assignmentsForTable );
			}
			assignmentsForTable.add( entry );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Add the root insert as cte


		final EntityPersister persister = entityDescriptor.getEntityPersister();
		final String rootTableName = persister.getTableName( 0 );
		final TableReference rootTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				rootTableName,
				true
		);

		final Generator identifierGenerator = entityDescriptor.getEntityPersister().getGenerator();
		final List<Map.Entry<List<CteColumn>, Assignment>> tableAssignments = assignmentsByTable.get( rootTableReference );
		if ( ( tableAssignments == null || tableAssignments.isEmpty() )
				&& !identifierGenerator.generatedOnExecution() ) {
			throw new IllegalStateException( "There must be at least a single root table assignment" );
		}

		final ConflictClause conflictClause = sqmConverter.visitConflictClause( sqmStatement.getConflictClause() );

		final int tableSpan = persister.getTableSpan();
		final String[] rootKeyColumns = persister.getKeyColumns( 0 );
		final List<CteColumn> keyCteColumns = queryCte.getCteTable().getCteColumns().subList( 0, rootKeyColumns.length );
		for ( int tableIndex = 0; tableIndex < tableSpan; tableIndex++ ) {
			final String tableExpression = persister.getTableName( tableIndex );
			final TableReference updatingTableReference = updatingTableGroup.getTableReference(
					updatingTableGroup.getNavigablePath(),
					tableExpression,
					true
			);
			final List<Map.Entry<List<CteColumn>, Assignment>> assignmentList = assignmentsByTable.get( updatingTableReference );
			final NamedTableReference dmlTableReference = resolveUnionTableReference(
					updatingTableReference,
					tableExpression
			);
			final String[] keyColumns = persister.getKeyColumns( tableIndex );
			final List<ColumnReference> returningColumnReferences = new ArrayList<>(
					keyColumns.length + ( assignmentList == null ? 0 : assignmentList.size() )
			);
			final List<ColumnReference> insertColumnReferences;
			final QuerySpec insertSelectSpec = new QuerySpec( true );
			CteStatement finalCteStatement = null;
			final CteTable dmlResultCte;
			if ( tableIndex == 0 && !assignsId && identifierGenerator.generatedOnExecution() ) {
				// Special handling for identity generation
				final String cteTableName = getCteTableName( tableExpression, "base_" );
				if ( statement.getCteStatement( cteTableName ) != null ) {
					// Since secondary tables could appear multiple times, we have to skip duplicates
					continue;
				}
				final String baseTableName = "base_" + queryCte.getCteTable().getTableExpression();
				insertSelectSpec.getFromClause().addRoot(
						new CteTableGroup(
								new NamedTableReference( baseTableName, "e" )
						)
				);
				final CteColumn rowNumberColumn = queryCte.getCteTable().getCteColumns().get(
						queryCte.getCteTable().getCteColumns().size() - 1
				);
				final ColumnReference rowNumberColumnReference = new ColumnReference(
						"e",
						rowNumberColumn.getColumnExpression(),
						false,
						null,
						rowNumberColumn.getJdbcMapping()
				);
				// Insert in the same order as the original tuples came
				insertSelectSpec.addSortSpecification(
						new SortSpecification(
								rowNumberColumnReference,
								SortDirection.ASCENDING
						)
				);
				dmlResultCte = new CteTable(
						cteTableName,
						keyCteColumns
				);
				for ( int j = 0; j < keyColumns.length; j++ ) {
					returningColumnReferences.add(
							new ColumnReference(
									dmlTableReference,
									keyColumns[j],
									false,
									null,
									null
							)
					);
				}
				insertColumnReferences = Collections.emptyList();
				final SelectStatement queryStatement = (SelectStatement) queryCte.getCteDefinition();
				final QuerySpec querySpec = queryStatement.getQuerySpec();

				final NavigablePath navigablePath = new NavigablePath( baseTableName );
				final TableGroup baseTableGroup = new TableGroupImpl(
						navigablePath,
						null,
						new NamedTableReference( baseTableName, "e" ),
						null
				);
				final TableGroup rootInsertCteTableGroup = new CteTableGroup(
						new NamedTableReference(
								getCteTableName( tableExpression ),
								"t"
						)
				);
				baseTableGroup.addTableGroupJoin(
						new TableGroupJoin(
								rootInsertCteTableGroup.getNavigablePath(),
								SqlAstJoinType.INNER,
								rootInsertCteTableGroup,
								new ComparisonPredicate(
										rowNumberColumnReference,
										ComparisonOperator.EQUAL,
										new ColumnReference(
												"t",
												rowNumberColumn.getColumnExpression(),
												false,
												null,
												rowNumberColumn.getJdbcMapping()
										)
								)
						)
				);
				querySpec.getFromClause().addRoot( baseTableGroup );
				final List<CteColumn> cteColumns = queryCte.getCteTable().getCteColumns();
				// The id column in this case comes from the dml CTE
				final CteColumn idCteColumn = cteColumns.get( 0 );
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								new ColumnReference(
										"t",
										idCteColumn.getColumnExpression(),
										false,
										null,
										idCteColumn.getJdbcMapping()
								)
						)
				);
				// The other columns come from the base CTE
				for ( int j = 1; j < cteColumns.size(); j++ ) {
					final CteColumn cteColumn = cteColumns.get( j );
					querySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									new ColumnReference(
											"e",
											cteColumn.getColumnExpression(),
											false,
											null,
											cteColumn.getJdbcMapping()
									)
							)
					);
				}

				// Now build the final CTE statement
				final List<CteColumn> finalReturningColumns = new ArrayList<>( keyCteColumns.size() + 1 );
				finalReturningColumns.addAll( keyCteColumns );
				finalReturningColumns.add( rowNumberColumn );
				final CteTable finalResultCte = new CteTable(
						getCteTableName( tableExpression ),
						finalReturningColumns
				);
				final QuerySpec finalResultQuery = new QuerySpec( true );
				finalResultQuery.getFromClause().addRoot(
						new CteTableGroup(
							new NamedTableReference(
									dmlResultCte.getTableExpression(),
									"e"
							)
						)
				);
				// The id column in this case comes from the dml CTE
				final ColumnReference idColumnReference = new ColumnReference(
						"e",
						idCteColumn.getColumnExpression(),
						false,
						null,
						idCteColumn.getJdbcMapping()
				);
				finalResultQuery.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								idColumnReference
						)
				);
				finalResultQuery.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								SqmInsertStrategyHelper.createRowNumberingExpression(
										querySpec,
										sessionFactory
								)
						)
				);
				finalResultQuery.addSortSpecification(
						new SortSpecification(
								idColumnReference,
								SortDirection.ASCENDING
						)
				);
				final SelectStatement finalResultStatement = new SelectStatement( finalResultQuery );
				finalCteStatement = new CteStatement( finalResultCte, finalResultStatement );
			}
			else {
				final String cteTableName = getCteTableName( tableExpression );
				if ( statement.getCteStatement( cteTableName ) != null ) {
					// Since secondary tables could appear multiple times, we have to skip duplicates
					continue;
				}
				insertSelectSpec.getFromClause().addRoot(
						new CteTableGroup(
								new NamedTableReference(
										queryCte.getCteTable().getTableExpression(),
										"e"
								)
						)
				);
				dmlResultCte = new CteTable(
						cteTableName,
						keyCteColumns
				);
				for ( int j = 0; j < keyColumns.length; j++ ) {
					returningColumnReferences.add(
							new ColumnReference(
									dmlTableReference,
									keyColumns[j],
									false,
									null,
									null
							)
					);
					insertSelectSpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									new ColumnReference(
											"e",
											rootKeyColumns[j],
											false,
											null,
											null
									)
							)
					);
				}
				insertColumnReferences = returningColumnReferences;
			}

			final InsertSelectStatement dmlStatement = new InsertSelectStatement(
					dmlTableReference,
					returningColumnReferences
			);
			dmlStatement.addTargetColumnReferences( insertColumnReferences );
			if ( assignmentList != null ) {
				for ( Map.Entry<List<CteColumn>, Assignment> entry : assignmentList ) {
					final Assignment assignment = entry.getValue();
					// Skip the id mapping here as we handled that already
					if ( assignment.getAssignedValue().getExpressionType() instanceof EntityIdentifierMapping ) {
						continue;
					}
					final List<ColumnReference> assignmentReferences = assignment.getAssignable().getColumnReferences();
					dmlStatement.addTargetColumnReferences( assignmentReferences );
					final int size = assignmentReferences.size();
					for ( int j = 0; j < size; j++ ) {
						final ColumnReference columnReference = assignmentReferences.get( j );
						insertSelectSpec.getSelectClause().addSqlSelection(
								new SqlSelectionImpl(
										new ColumnReference(
												"e",
												entry.getKey().get( j ).getColumnExpression(),
												columnReference.isColumnExpressionFormula(),
												null,
												columnReference.getJdbcMapping()
										)
								)
						);
					}
				}
			}
			dmlStatement.setSourceSelectStatement( insertSelectSpec );
			if ( conflictClause != null ) {
				if ( conflictClause.isDoNothing() && conflictClause.getConstraintColumnNames().isEmpty() ) {
					// Conflict clauses that use a constraint name and do nothing can just use the conflict clause as it is
					handleConflictClause( dmlResultCte, dmlStatement, queryCte, tableIndex, conflictClause, statement );
				}
				else {
					final List<Assignment> compatibleAssignments = getCompatibleAssignments( dmlStatement, conflictClause );
					if ( isIdentifierConflictClause( sqmStatement ) ) {
						// If the identifier is used in the SqmInsert, use the key columns of the respective table
						handleConflictClause(
								dmlResultCte,
								dmlStatement,
								queryCte,
								tableIndex,
								new ConflictClause(
										conflictClause.getConstraintName(),
										Arrays.asList( keyColumns ),
										compatibleAssignments,
										compatibleAssignments.isEmpty() ? null : conflictClause.getPredicate()
								),
								statement
						);
					}
					else if ( targetColumnsContainAllConstraintColumns( dmlStatement, conflictClause ) ) {
						// Also apply the conflict clause if the insert target columns contain the constraint columns
						handleConflictClause(
								dmlResultCte,
								dmlStatement,
								queryCte,
								tableIndex,
								new ConflictClause(
										conflictClause.getConstraintName(),
										conflictClause.getConstraintColumnNames(),
										compatibleAssignments,
										compatibleAssignments.isEmpty() ? null : conflictClause.getPredicate()
								),
								statement
						);
					}
					else {
						statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
					}
				}
			}
			else {
				statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
			}
			if ( finalCteStatement != null ) {
				statement.addCteStatement( finalCteStatement );
			}
			if ( tableIndex == 0 && !assignsId && identifierGenerator.generatedOnExecution() ) {
				// Special handling for identity generation
				statement.addCteStatement( queryCte );
			}
		}
		return getCteTableName( rootTableName );
	}

	private void handleConflictClause(
			CteTable dmlResultCte,
			InsertSelectStatement insertStatement,
			CteStatement queryCte,
			int tableIndex,
			ConflictClause conflictClause,
			CteContainer statement) {
		if ( sessionFactory.getJdbcServices().getDialect().supportsConflictClauseForInsertCTE() ) {
			insertStatement.setConflictClause( conflictClause );
			statement.addCteStatement( new CteStatement( dmlResultCte, insertStatement ) );
		}
		else {
			// Build an exists subquery clause to only insert if no row with a matching constraint column value exists i.e.
			// insert into target (c1, c2)
			// select e.c1, e.c2 from HTE_target e
			// where not exists (select 1 from target excluded where e.c1=excluded.c1 and e.c2=excluded.c2)
			final BasicType<Boolean> booleanType = sessionFactory.getNodeBuilder().getBooleanType();
			final List<String> constraintColumnNames = conflictClause.getConstraintColumnNames();
			final QuerySpec insertQuerySpec = (QuerySpec) insertStatement.getSourceSelectStatement();
			final QuerySpec subquery = new QuerySpec( false, 1 );
			// This is the table group we use in the subquery to check no row for the given constraint columns exists.
			// We name it "excluded" because the predicates we build for this check are reused for the
			// check in the update statement below.
			// "excluded" is our well known name to refer to data that was not inserted
			final TableGroup tableGroup = new StandardTableGroup(
					false,
					new NavigablePath( "excluded" ),
					entityDescriptor,
					null,
					new NamedTableReference(
							insertStatement.getTargetTable().getTableExpression(),
							"excluded"
					),
					null,
					sessionFactory
			);
			subquery.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( new QueryLiteral<>( 1, sessionFactory.getNodeBuilder().getIntegerType() ) )
			);
			subquery.getFromClause().addRoot( tableGroup );
			List<String> columnsToMatch;
			if ( constraintColumnNames.isEmpty() ) {
				// Assume the primary key columns
				Predicate predicate = buildColumnMatchPredicate(
						columnsToMatch = Arrays.asList( ( (EntityPersister) entityDescriptor).getKeyColumns( tableIndex ) ),
						insertStatement,
						false,
						true
				);
				if ( predicate == null ) {
					throw new IllegalArgumentException( "Couldn't infer conflict constraint columns" );
				}
				subquery.applyPredicate( predicate );
			}
			else {
				columnsToMatch = constraintColumnNames;
				subquery.applyPredicate( buildColumnMatchPredicate( constraintColumnNames, insertStatement, true, true ) );
			}

			insertQuerySpec.applyPredicate( new ExistsPredicate( subquery, true, booleanType ) );

			// Emulate the conflict do update clause by creating a separate update CTEs
			if ( conflictClause.isDoUpdate() ) {
				final TableGroup temporaryTableGroup = insertQuerySpec.getFromClause().getRoots().get( 0 );
				final QuerySpec renamingSubquery = new QuerySpec( false, 1 );
				final List<String> columnNames = buildCteRenaming(
						renamingSubquery,
						temporaryTableGroup,
						queryCte
				);
				renamingSubquery.getFromClause().addRoot( temporaryTableGroup );
				final QueryPartTableGroup excludedTableGroup = new QueryPartTableGroup(
						new NavigablePath( "excluded" ),
						null,
						new SelectStatement( renamingSubquery ),
						"excluded",
						columnNames,
						false,
						false,
						sessionFactory
				);

				final UpdateStatement updateStatement;
				if ( sessionFactory.getJdbcServices().getDialect().supportsFromClauseInUpdate() ) {
					final FromClause fromClause = new FromClause( 1 );
					final TableGroup updateTableGroup = new StandardTableGroup(
							false,
							new NavigablePath( "updated" ),
							entityDescriptor,
							null,
							insertStatement.getTargetTable(),
							null,
							sessionFactory
					);
					fromClause.addRoot( updateTableGroup );
					updateStatement = new UpdateStatement(
							insertStatement.getTargetTable(),
							fromClause,
							conflictClause.getAssignments(),
							conflictClause.getPredicate(),
							insertStatement.getReturningColumns()
					);
					updateTableGroup.addTableGroupJoin(
							new TableGroupJoin(
									excludedTableGroup.getNavigablePath(),
									SqlAstJoinType.INNER,
									excludedTableGroup,
									buildColumnMatchPredicate(
											columnsToMatch,
											insertStatement,
											true,
											false
									)
							)
					);
				}
				else {
					final List<Assignment> assignments = conflictClause.getAssignments();
					final List<ColumnReference> assignmentColumns = new ArrayList<>( assignments.size() );
					final QuerySpec updateSubquery = new QuerySpec( false, 1 );
					for ( Assignment assignment : assignments ) {
						assignmentColumns.add( (ColumnReference) assignment.getAssignable() );
						updateSubquery.getSelectClause().addSqlSelection(
								new SqlSelectionImpl( assignment.getAssignedValue() )
						);
					}
					updateSubquery.getFromClause().addRoot( excludedTableGroup );
					updateSubquery.applyPredicate( buildColumnMatchPredicate(
							columnsToMatch,
							insertStatement,
							true,
							false
					) );
					final QuerySpec matchCteSubquery = new QuerySpec( false, 1 );
					matchCteSubquery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl( new QueryLiteral<>(
									1,
									sessionFactory.getNodeBuilder().getIntegerType()
							) )
					);
					matchCteSubquery.getFromClause().addRoot( updateSubquery.getFromClause().getRoots().get( 0 ) );
					matchCteSubquery.applyPredicate( updateSubquery.getWhereClauseRestrictions() );
					updateStatement = new UpdateStatement(
							insertStatement.getTargetTable(),
							List.of( new Assignment(
									new SqlTuple( assignmentColumns, null ),
									new SelectStatement( updateSubquery )
							) ),
							Predicate.combinePredicates(
									new ExistsPredicate( matchCteSubquery, false, booleanType ),
									conflictClause.getPredicate()
							),
							insertStatement.getReturningColumns()
					);
				}

				final CteTable updateCte = dmlResultCte.withName( dmlResultCte.getTableExpression() + "_upd" );
				statement.addCteStatement( new CteStatement( updateCte, updateStatement ) );

				final CteTable insertCte = dmlResultCte.withName( dmlResultCte.getTableExpression() + "_ins" );
				statement.addCteStatement( new CteStatement( insertCte, insertStatement ) );

				// Union the update and inserted ids together to be able to determine the effective update count
				final List<QueryPart> queryParts = new ArrayList<>( 2 );
				final QuerySpec dmlCombinationQ1 = new QuerySpec( false, 1 );
				final QuerySpec dmlCombinationQ2 = new QuerySpec( false, 1 );
				dmlCombinationQ1.getSelectClause().addSqlSelection( new SqlSelectionImpl( new Star() ) );
				dmlCombinationQ2.getSelectClause().addSqlSelection( new SqlSelectionImpl( new Star() ) );
				dmlCombinationQ1.getFromClause().addRoot( new CteTableGroup( new NamedTableReference( updateCte.getTableExpression(), "t" ) ) );
				dmlCombinationQ2.getFromClause().addRoot( new CteTableGroup( new NamedTableReference( insertCte.getTableExpression(), "t" ) ) );
				queryParts.add( dmlCombinationQ1 );
				queryParts.add( dmlCombinationQ2 );
				final SelectStatement dmlCombinationStatement = new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, queryParts ) );
				statement.addCteStatement( new CteStatement( dmlResultCte, dmlCombinationStatement ) );
			}
			else {
				statement.addCteStatement( new CteStatement( dmlResultCte, insertStatement ) );
			}
		}
	}

	private List<String> buildCteRenaming(
			QuerySpec renamingSubquery,
			TableGroup temporaryTableGroup,
			CteStatement queryCte) {
		final List<CteColumn> cteColumns = queryCte.getCteTable().getCteColumns();
		for ( CteColumn cteColumn : cteColumns ) {
			renamingSubquery.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							new ColumnReference(
									temporaryTableGroup.getPrimaryTableReference(),
									cteColumn.getColumnExpression(),
									cteColumn.getJdbcMapping()
							)
					)
			);
		}
		final SelectStatement selectStatement = (SelectStatement) queryCte.getCteDefinition();
		final QuerySpec querySpec = (QuerySpec) selectStatement.getQueryPart();
		final DerivedTableReference tableReference = (DerivedTableReference) querySpec.getFromClause()
				.getRoots()
				.get( 0 )
				.getPrimaryTableReference();
		return tableReference.getColumnNames();
	}

	private Predicate buildColumnMatchPredicate(
			List<String> constraintColumnNames,
			InsertSelectStatement dmlStatement,
			boolean errorIfMissing,
			boolean compareAgainstSelectItems) {
		final BasicType<Boolean> booleanType = sessionFactory.getNodeBuilder().getBooleanType();
		final QuerySpec insertQuerySpec = (QuerySpec) dmlStatement.getSourceSelectStatement();
		Predicate predicate = null;
		OUTER: for ( String constraintColumnName : constraintColumnNames ) {
			final List<ColumnReference> targetColumns = dmlStatement.getTargetColumns();
			for ( int i = 0; i < targetColumns.size(); i++ ) {
				final ColumnReference columnReference = targetColumns.get( i );
				if ( columnReference.getColumnExpression().equals( constraintColumnName ) ) {
					if ( compareAgainstSelectItems ) {
						predicate = Predicate.combinePredicates(
								predicate,
								new ComparisonPredicate(
										new ColumnReference(
												"excluded",
												columnReference.getColumnExpression(),
												false,
												null,
												columnReference.getJdbcMapping()
										),
										ComparisonOperator.EQUAL,
										insertQuerySpec.getSelectClause()
												.getSqlSelections()
												.get( i )
												.getExpression(),
										booleanType
								)
						);
					}
					else {
						predicate = Predicate.combinePredicates(
								predicate,
								new ComparisonPredicate(
										columnReference,
										ComparisonOperator.EQUAL,
										new ColumnReference(
												"excluded",
												columnReference.getColumnExpression(),
												false,
												null,
												columnReference.getJdbcMapping()
										),
										booleanType
								)
						);
					}
					continue OUTER;
				}
			}
			if ( errorIfMissing ) {
				// Should never happen
				final List<String> targetColumnNames = targetColumns.stream()
						.map( ColumnReference::getColumnExpression )
						.collect( Collectors.toList() );
				throw new IllegalArgumentException( "Couldn't find conflict constraint column [" + constraintColumnName + "] in insert target columns: " + targetColumnNames );
			}
			return null;
		}
		return predicate;
	}

	private List<Assignment> getCompatibleAssignments(InsertSelectStatement dmlStatement, ConflictClause conflictClause) {
		if ( conflictClause.isDoNothing() ) {
			return Collections.emptyList();
		}
		List<Assignment> compatibleAssignments = null;
		final List<Assignment> assignments = conflictClause.getAssignments();
		for ( Assignment assignment : assignments ) {
			for ( ColumnReference targetColumn : dmlStatement.getTargetColumns() ) {
				if ( assignment.getAssignable().getColumnReferences().contains( targetColumn ) ) {
					if ( compatibleAssignments == null ) {
						compatibleAssignments = new ArrayList<>( assignments.size() );
					}
					compatibleAssignments.add( assignment );
					break;
				}
			}
		}
		return compatibleAssignments == null ? Collections.emptyList() : compatibleAssignments;
	}

	private boolean isIdentifierConflictClause(SqmInsertStatement<?> sqmStatement) {
		final SqmConflictClause<?> conflictClause = sqmStatement.getConflictClause();
		assert conflictClause != null;
		final List<SqmPath<?>> constraintPaths = conflictClause.getConstraintPaths();
		return constraintPaths.size() == 1
				&& constraintPaths.get( 0 ).getReferencedPathSource() == sqmStatement.getTarget().getModel().getIdentifierDescriptor();
	}

	private boolean targetColumnsContainAllConstraintColumns(InsertSelectStatement statement, ConflictClause conflictClause) {
		OUTER: for ( String constraintColumnName : conflictClause.getConstraintColumnNames() ) {
			for ( ColumnReference targetColumn : statement.getTargetColumns() ) {
				if ( targetColumn.getColumnExpression().equals( constraintColumnName ) ) {
					continue OUTER;
				}
			}
			return false;
		}

		return true;
	}

	protected NamedTableReference resolveUnionTableReference(
			TableReference tableReference,
			String tableExpression) {
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

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	protected String getCteTableName(String tableExpression) {
		return getCteTableName( tableExpression, "" );
	}

	protected String getCteTableName(String tableExpression, String subPrefix) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		if ( Identifier.isQuoted( tableExpression ) ) {
			tableExpression = tableExpression.substring( 1, tableExpression.length() - 1 );
		}
		return Identifier.toIdentifier( DML_RESULT_TABLE_NAME_PREFIX + subPrefix + tableExpression ).render( dialect );
	}
}
