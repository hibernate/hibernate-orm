/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.NumericTypeCategory;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


/**
 * Recursive CTE based generate_series function.
 */
public class CteGenerateSeriesFunction extends NumberSeriesGenerateSeriesFunction {

	public CteGenerateSeriesFunction(int maxSeriesSize, boolean supportsIntervals, boolean coerceToTimestamp, TypeConfiguration typeConfiguration) {
		super(
				new CteGenerateSeriesSetReturningFunctionTypeResolver(),
				// Treat durations like intervals to avoid conversions
				typeConfiguration.getBasicTypeRegistry().resolve(
						java.time.Duration.class,
						supportsIntervals ? SqlTypes.INTERVAL_SECOND : SqlTypes.DURATION
				),
				coerceToTimestamp,
				maxSeriesSize
		);
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> arguments, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SelfRenderingSqmSetReturningFunction<>(
				this,
				this,
				arguments,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {
			@Override
			public TableGroup convertToSqlAst(NavigablePath navigablePath, String identifierVariable, boolean lateral, boolean canUseInnerJoins, boolean withOrdinality, SqmToSqlAstConverter walker) {
				final FunctionTableGroup tableGroup = (FunctionTableGroup) super.convertToSqlAst(
						navigablePath,
						identifierVariable,
						lateral,
						canUseInnerJoins,
						withOrdinality,
						walker
				);
				final AnonymousTupleTableGroupProducer tableGroupProducer = (AnonymousTupleTableGroupProducer) tableGroup.getModelPart();
				if ( !lateral ) {
					return new QueryPartTableGroup(
							navigablePath,
							tableGroupProducer,
							createCteSubquery( tableGroup, walker ),
							identifierVariable,
							tableGroupProducer.getColumnNames(),
							tableGroup.getPrimaryTableReference().getCompatibleTableExpressions(),
							lateral,
							canUseInnerJoins,
							walker.getCreationContext().getSessionFactory()
					);
				}
				else {
					final CteTableGroup cteTableGroup = new CteTableGroup(
							canUseInnerJoins,
							navigablePath,
							null,
							tableGroupProducer,
							new NamedTableReference( CteGenerateSeriesQueryTransformer.NAME, identifierVariable ),
							tableGroupProducer.getCompatibleTableExpressions()
					);
					walker.registerQueryTransformer( new CteGenerateSeriesQueryTransformer(
							tableGroup,
							cteTableGroup,
							maxSeriesSize,
							"i",
							coerceToTimestamp
					) );
					return cteTableGroup;
				}
			}
		};
	}

	public static class CteGenerateSeriesQueryTransformer extends NumberSeriesQueryTransformer {

		public static final String NAME = "max_series";
		protected final int maxSeriesSize;

		public CteGenerateSeriesQueryTransformer(FunctionTableGroup functionTableGroup, TableGroup targetTableGroup, int maxSeriesSize, String positionColumnName, boolean coerceToTimestamp) {
			super( functionTableGroup, targetTableGroup, positionColumnName, coerceToTimestamp );
			this.maxSeriesSize = maxSeriesSize;
		}

		@Override
		public QuerySpec transform(CteContainer cteContainer, QuerySpec querySpec, SqmToSqlAstConverter converter) {
			// First add the CTE that creates the series
			if ( cteContainer.getCteStatement( CteGenerateSeriesQueryTransformer.NAME ) == null ) {
				cteContainer.addCteStatement( createSeriesCte( converter ) );
			}
			return super.transform( cteContainer, querySpec, converter );
		}

		protected CteStatement createSeriesCte(SqmToSqlAstConverter converter) {
			return createSeriesCte( maxSeriesSize, converter );
		}

		public static CteStatement createSeriesCte(int maxSeriesSize, SqmToSqlAstConverter converter) {
			final BasicType<Long> longType =
					converter.getCreationContext().getTypeConfiguration()
							.getBasicTypeForJavaType( Long.class );
			final Expression one = new UnparsedNumericLiteral<>( "1", NumericTypeCategory.LONG, longType );
			final List<CteColumn> cteColumns = List.of( new CteColumn( "i", longType ) );

			final QuerySpec cteStart = new QuerySpec( false );
			cteStart.getSelectClause().addSqlSelection( new SqlSelectionImpl( one ) );

			final QuerySpec cteUnion = new QuerySpec( false );
			final CteTableGroup cteTableGroup = new CteTableGroup( new NamedTableReference( CteGenerateSeriesQueryTransformer.NAME, "t" ) );
			cteUnion.getFromClause().addRoot( cteTableGroup );
			final ColumnReference tIndex = new ColumnReference( cteTableGroup.getPrimaryTableReference(), "i", longType );
			final Expression nextValue = new BinaryArithmeticExpression(
					tIndex,
					BinaryArithmeticOperator.ADD,
					one,
					longType
			);
			cteUnion.getSelectClause().addSqlSelection( new SqlSelectionImpl( nextValue ) );
			cteUnion.applyPredicate(
					new ComparisonPredicate(
							nextValue,
							ComparisonOperator.LESS_THAN_OR_EQUAL,
							new UnparsedNumericLiteral<>(
									Integer.toString( maxSeriesSize ),
									NumericTypeCategory.LONG,
									longType
							)
					)
			);
			final QueryGroup cteContent = new QueryGroup( false, SetOperator.UNION_ALL, List.of( cteStart, cteUnion ) );
			final CteStatement cteStatement = new CteStatement(
					new CteTable( CteGenerateSeriesQueryTransformer.NAME, cteColumns ),
					new SelectStatement( cteContent )
			);
			cteStatement.setRecursive();
			return cteStatement;
		}
	}

	private SelectStatement createCteSubquery(FunctionTableGroup tableGroup, SqmToSqlAstConverter walker) {
		final AnonymousTupleTableGroupProducer tableGroupProducer = (AnonymousTupleTableGroupProducer) tableGroup.getModelPart();
		final ModelPart indexPart = tableGroupProducer.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		final ModelPart elementPart = tableGroupProducer.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null );
		final NumericTypeCategory numericTypeCategory = NumericTypeCategory.BIG_DECIMAL;
		final BasicType<?> resultType = (BasicType<?>) elementPart.getSingleJdbcMapping();
		final BasicType<Integer> integerType = walker.getCreationContext().getTypeConfiguration()
				.getBasicTypeForJavaType( Integer.class );
		final BasicType<Boolean> booleanType = walker.getCreationContext().getTypeConfiguration()
				.getBasicTypeForJavaType( Boolean.class );

		final JdbcType boundType = resultType.getJdbcType();
		final boolean castTimestamp = coerceToTimestamp
				&& (boundType.getDdlTypeCode() == SqlTypes.DATE || boundType.getDdlTypeCode() == SqlTypes.TIME);

		final List<? extends SqlAstNode> arguments = tableGroup.getPrimaryTableReference().getFunctionExpression()
				.getArguments();
		final Expression start = castTimestamp
				? castToTimestamp( arguments.get( 0 ), walker )
				: (Expression) arguments.get( 0 );
		final Expression stop = castTimestamp
				? castToTimestamp( arguments.get( 1 ), walker )
				: (Expression) arguments.get( 1 );
		final Expression explicitStep = arguments.size() > 2 ? (Expression) arguments.get( 2 ) : null;
		final Expression step = explicitStep != null
				? explicitStep
				: new UnparsedNumericLiteral<>( "1", numericTypeCategory, resultType );

		final String cteName = "generate_series";
		final List<CteColumn> cteColumns;
		if ( indexPart == null ) {
			cteColumns = List.of( new CteColumn( "v", resultType ) );
		}
		else {
			cteColumns = List.of(
					new CteColumn( "v", resultType ),
					new CteColumn( "i", indexPart.getSingleJdbcMapping() )
			);
		}

		// Select the start value and check if the step can progress towards the stop value
		final QuerySpec cteStart = new QuerySpec( false );
		if ( explicitStep == null ) {
			cteStart.getSelectClause().addSqlSelection( new SqlSelectionImpl( start ) );
		}
		else {
			// For explicit steps, we need to add the step 0 times in the initial part of the recursive CTE,
			// in order for the database to recognize the correct result type of the CTE column
			cteStart.getSelectClause().addSqlSelection( new SqlSelectionImpl( add(
					start,
					multiply( step, 0, integerType ),
					walker
			) ) );
		}

		if ( indexPart != null ) {
			// ordinal is 1 based
			cteStart.getSelectClause().addSqlSelection( new SqlSelectionImpl(
					new UnparsedNumericLiteral<>( "1", NumericTypeCategory.INTEGER, integerType )
			) );
		}

		// Add a predicate to ensure the start value is valid
		if ( explicitStep == null ) {
			// The default step is 1, so just check if start <= stop
			cteStart.applyPredicate(
					new ComparisonPredicate(
							start,
							ComparisonOperator.LESS_THAN_OR_EQUAL,
							stop
					)
			);
		}
		else {
			// When start <= stop, only produce an initial result if the step is positive i.e. step > step*-1
			final Predicate positiveProgress = new Junction(
					Junction.Nature.CONJUNCTION,
					List.of(
							new ComparisonPredicate(
									start,
									ComparisonOperator.LESS_THAN_OR_EQUAL,
									stop
							),
							new ComparisonPredicate(
									step,
									ComparisonOperator.GREATER_THAN,
									multiply( step, -1, integerType )
							)
					),
					booleanType
			);
			// When start >= stop, only produce an initial result if the step is negative i.e. step > step*-1
			final Predicate negativeProgress = new Junction(
					Junction.Nature.CONJUNCTION,
					List.of(
							new ComparisonPredicate(
									start,
									ComparisonOperator.GREATER_THAN_OR_EQUAL,
									stop
							),
							new ComparisonPredicate(
									step,
									ComparisonOperator.LESS_THAN,
									multiply( step, -1, integerType )
							)
					),
					booleanType
			);
			cteStart.applyPredicate(
					new Junction(
							Junction.Nature.DISJUNCTION,
							List.of( positiveProgress, negativeProgress ),
							booleanType
					)
			);
		}

		// The union part just adds the step to the previous value as long as the stop value is not reached
		final QuerySpec cteUnion = new QuerySpec( false );
		final CteTableGroup cteTableGroup = new CteTableGroup( new NamedTableReference( cteName, "t" ) );
		cteUnion.getFromClause().addRoot( cteTableGroup );
		final ColumnReference tValue = new ColumnReference( cteTableGroup.getPrimaryTableReference(), "v", resultType );
		final ColumnReference tIndex = indexPart == null
				? null
				: new ColumnReference(
						cteTableGroup.getPrimaryTableReference(),
						"i",
						indexPart.getSingleJdbcMapping()
				);
		final Expression nextValue = add( tValue, step, walker );
		cteUnion.getSelectClause().addSqlSelection( new SqlSelectionImpl( nextValue ) );
		if ( tIndex != null ) {
			cteUnion.getSelectClause().addSqlSelection( new SqlSelectionImpl( new BinaryArithmeticExpression(
					tIndex,
					BinaryArithmeticOperator.ADD,
					new UnparsedNumericLiteral<>( "1", NumericTypeCategory.INTEGER, integerType ),
					(BasicValuedMapping) indexPart.getSingleJdbcMapping()
			) ) );
		}

		// Add a predicate to ensure the current value is valid
		if ( explicitStep == null ) {
			// The default step is 1, so just check if value <= stop
			cteUnion.applyPredicate(
					new ComparisonPredicate(
							nextValue,
							ComparisonOperator.LESS_THAN_OR_EQUAL,
							stop
					)
			);
		}
		else {
			// When start < stop, value is only valid if it's less than or equal to stop
			final Predicate positiveProgress = new Junction(
					Junction.Nature.CONJUNCTION,
					List.of(
							new ComparisonPredicate(
									start,
									ComparisonOperator.LESS_THAN,
									stop
							),
							new ComparisonPredicate(
									nextValue,
									ComparisonOperator.LESS_THAN_OR_EQUAL,
									stop
							)
					),
					booleanType
			);
			// When start > stop, value is only valid if it's greater than or equal to stop
			final Predicate negativeProgress = new Junction(
					Junction.Nature.CONJUNCTION,
					List.of(
							new ComparisonPredicate(
									start,
									ComparisonOperator.GREATER_THAN,
									stop
							),
							new ComparisonPredicate(
									nextValue,
									ComparisonOperator.GREATER_THAN_OR_EQUAL,
									stop
							)
					),
					booleanType
			);
			cteUnion.applyPredicate(
					new Junction(
							Junction.Nature.DISJUNCTION,
							List.of( positiveProgress, negativeProgress ),
							booleanType
					)
			);
		}

		// Main query selects the columns from the CTE
		final QueryGroup cteContent = new QueryGroup( false, SetOperator.UNION_ALL, List.of( cteStart, cteUnion ) );
		final QuerySpec mainQuery = new QuerySpec( false );
		final SelectStatement selectStatement = new SelectStatement( mainQuery );
		final CteStatement cteStatement = new CteStatement(
				new CteTable( cteName, cteColumns ),
				new SelectStatement( cteContent )
		);
		cteStatement.setRecursive();
		selectStatement.addCteStatement( cteStatement );
		mainQuery.getFromClause().addRoot( cteTableGroup );
		mainQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( tValue ) );
		if ( indexPart != null ) {
			mainQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( tIndex ) );
		}
		return selectStatement;
	}

	static class CteGenerateSeriesSetReturningFunctionTypeResolver extends NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver {

		public CteGenerateSeriesSetReturningFunctionTypeResolver() {
			super( "v", "i" );
		}

		public CteGenerateSeriesSetReturningFunctionTypeResolver(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression) {
			super( defaultValueColumnName, defaultIndexSelectionExpression );
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			if ( !lateral ) {
				return super.resolveFunctionReturnType( arguments, tableIdentifierVariable, lateral, withOrdinality, converter );
			}
			else {
				return resolveIterationVariableBasedFunctionReturnType( arguments, tableIdentifierVariable, lateral, withOrdinality, converter );
			}
		}
	}

	@Override
	protected void renderGenerateSeries(
			SqlAppender sqlAppender,
			Expression start,
			Expression stop,
			@Nullable Expression step,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		throw new UnsupportedOperationException( "Function expands to custom SQL AST" );
	}
}
