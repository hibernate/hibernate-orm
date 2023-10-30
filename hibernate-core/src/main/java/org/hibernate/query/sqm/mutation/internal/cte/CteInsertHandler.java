/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.TemporaryTable;
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
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.InsertHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
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
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableGroup;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.generator.Generator;
import org.hibernate.type.spi.TypeConfiguration;

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

	public static CteTable createCteTable(
			CteTable sqmCteTable,
			List<CteColumn> sqmCteColumns,
			SessionFactoryImplementor factory) {
		return new CteTable(
				sqmCteTable.getTableExpression(),
				sqmCteColumns
		);
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
		final String explicitDmlTargetAlias;
		if ( sqmInsertStatement.getTarget().getExplicitAlias() == null ) {
			explicitDmlTargetAlias = "dml_target";
		}
		else {
			explicitDmlTargetAlias = sqmInsertStatement.getTarget().getExplicitAlias();
		}

		final MultiTableSqmMutationConverter sqmConverter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmInsertStatement,
				sqmInsertStatement.getTarget(),
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
		final NamedTableReference entityTableReference = new NamedTableReference(
				cteTable.getTableExpression(),
				TemporaryTable.DEFAULT_ALIAS,
				true
		);
		final InsertSelectStatement insertStatement = new InsertSelectStatement( entityTableReference );

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
					insertStatement.addTargetColumnReferences( columnReferences );
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
							final ColumnReference columnReference = new ColumnReference(
									(String) null,
									rowNumberColumn.getColumnExpression(),
									false,
									null,
									rowNumberColumn.getJdbcMapping()
							);
							insertStatement.getTargetColumns().set(
									insertStatement.getTargetColumns().size() - 1,
									columnReference
							);
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
			final ColumnReference columnReference = new ColumnReference(
					(String) null,
					rowNumberColumn.getColumnExpression(),
					false,
					null,
					rowNumberColumn.getJdbcMapping()
			);
			insertStatement.getTargetColumns().add( columnReference );
			targetPathCteColumns.add( rowNumberColumn );
		}

		final CteTable entityCteTable = createCteTable(
				getCteTable(),
				targetPathCteColumns,
				factory
		);

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
				final String fragment = ( (BulkInsertionCapableIdentifierGenerator) entityDescriptor.getGenerator() )
						.determineBulkInsertionIdentifierGenerationSelectFragment(
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
					finalEntityCteTable = createCteTable(
							getCteTable(),
							targetPathCteColumns,
							factory
					);
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
			final CteTable finalEntityCteTable = createCteTable(
					getCteTable(),
					targetPathCteColumns,
					factory
			);
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
				sqmConverter.getJdbcParamsBySqmParam(),
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
				SqmUtil.generateJdbcParamsXref(domainParameterXref, sqmConverter),
				factory.getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> sqmConverter.getMutatingTableGroup(),
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
				row -> row[0],
				ListResultsConsumer.UniqueSemantic.NONE
		);
		return ( (Number) list.get( 0 ) ).intValue();
	}

	protected Expression createCountStar(
			SessionFactoryImplementor factory,
			MultiTableSqmMutationConverter sqmConverter) {
		final SqmExpression<?> arg = new SqmStar( factory.getNodeBuilder() );
		final TypeConfiguration typeConfiguration = factory.getJpaMetamodel().getTypeConfiguration();
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
			Map<SqmParameter<?>, List<List<JdbcParameter>>> parameterResolutions,
			SessionFactoryImplementor factory) {
		final TableGroup updatingTableGroup = sqmConverter.getMutatingTableGroup();
		final EntityMappingType entityDescriptor = getEntityDescriptor();

		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		final String rootEntityName = entityPersister.getRootEntityName();
		final EntityPersister rootEntityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = ( (Joinable) rootEntityDescriptor ).getTableName();
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
				final TableReference tableReference = resolveTableReference(
						columnReference,
						updatingTableGroup,
						tableReferenceByAlias
				);

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


		final AbstractEntityPersister persister = (AbstractEntityPersister) entityDescriptor.getEntityPersister();
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

		final int tableSpan = persister.getTableSpan();
		final String[] rootKeyColumns = persister.getKeyColumns( 0 );
		final List<CteColumn> keyCteColumns = queryCte.getCteTable().getCteColumns().subList( 0, rootKeyColumns.length );
		for ( int i = 0; i < tableSpan; i++ ) {
			final String tableExpression = persister.getTableName( i );
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
			final String[] keyColumns = persister.getKeyColumns( i );
			final List<ColumnReference> returningColumnReferences = new ArrayList<>(
					keyColumns.length + ( assignmentList == null ? 0 : assignmentList.size() )
			);
			final List<ColumnReference> insertColumnReferences;
			final QuerySpec insertSelectSpec = new QuerySpec( true );
			CteStatement finalCteStatement = null;
			final CteTable dmlResultCte;
			if ( i == 0 && !assignsId && identifierGenerator.generatedOnExecution() ) {
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
			statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
			if ( finalCteStatement != null ) {
				statement.addCteStatement( finalCteStatement );
			}
			if ( i == 0 && !assignsId && identifierGenerator.generatedOnExecution() ) {
				// Special handling for identity generation
				statement.addCteStatement( queryCte );
			}
		}
		return getCteTableName( rootTableName );
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
			TableGroup updatingTableGroup,
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
