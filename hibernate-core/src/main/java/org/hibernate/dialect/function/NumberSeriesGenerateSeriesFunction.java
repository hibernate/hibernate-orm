/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.NumericTypeCategory;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;

/**
 * The base for generate_series function implementations that use a static number source.
 */
public abstract class NumberSeriesGenerateSeriesFunction extends GenerateSeriesFunction {

	protected final int maxSeriesSize;

	public NumberSeriesGenerateSeriesFunction(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression, boolean coerceToTimestamp, TypeConfiguration typeConfiguration, int maxSeriesSize) {
		super( defaultValueColumnName, defaultIndexSelectionExpression, coerceToTimestamp, typeConfiguration );
		this.maxSeriesSize = maxSeriesSize;
	}

	public NumberSeriesGenerateSeriesFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, BasicType<Duration> durationType, int maxSeriesSize) {
		super( setReturningFunctionTypeResolver, durationType );
		this.maxSeriesSize = maxSeriesSize;
	}

	public NumberSeriesGenerateSeriesFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, BasicType<Duration> durationType, boolean coerceToTimestamp, int maxSeriesSize) {
		super( setReturningFunctionTypeResolver, durationType, coerceToTimestamp );
		this.maxSeriesSize = maxSeriesSize;
	}

	@Override
	protected abstract void renderGenerateSeries(
			SqlAppender sqlAppender,
			Expression start,
			Expression stop,
			@Nullable Expression step,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker);

	/**
	 * Returns whether a variable (e.g. through values clause) shall be introduced for an expression,
	 * which is passed as argument to the {@code generate_series} function.
	 * Since the selection expression of the value column that this function returns must be transformed
	 * to the form {@code start + step * ( iterationVariable - 1 )}, it is vital that {@code start} and {@code step}
	 * can be rendered to a {@code String} during SQL AST build time for {@link SelectableMapping#getSelectionExpression()}.
	 * If that isn't possible because the expression is too complex, a variable needs to be introduced which is then used
	 * instead of the original expression.
	 */
	protected static boolean needsVariable(Expression expression) {
		return !( expression instanceof Literal || expression instanceof ColumnReference );
	}

	public static Expression add(Expression left, Expression right, SqmToSqlAstConverter converter) {
		if ( right instanceof org.hibernate.sql.ast.tree.expression.Duration duration ) {
			final BasicType<?> nodeType = (BasicType<?>) left.getExpressionType().getSingleJdbcMapping();
			final FunctionRenderer timestampadd = (FunctionRenderer) converter.getCreationContext()
					.getSqmFunctionRegistry().findFunctionDescriptor( "timestampadd" );
			return new SelfRenderingFunctionSqlAstExpression<>(
					"timestampadd",
					timestampadd,
					List.of(
							new DurationUnit( duration.getUnit(), duration.getExpressionType() ),
							duration.getMagnitude(),
							left
					),
					nodeType,
					nodeType
			);
		}
		else {
			return new BinaryArithmeticExpression(
					left,
					BinaryArithmeticOperator.ADD,
					right,
					(BasicValuedMapping) left.getExpressionType()
			);
		}
	}

	public static Expression multiply(Expression left, int multiplier, BasicType<Integer> integerType) {
		return multiply( left, new UnparsedNumericLiteral<>( Integer.toString( multiplier ), NumericTypeCategory.INTEGER, integerType ) );
	}

	public static Expression multiply(Expression left, Expression multiplier) {
		if ( left instanceof org.hibernate.sql.ast.tree.expression.Duration duration ) {
			return new org.hibernate.sql.ast.tree.expression.Duration(
					multiply( duration.getMagnitude(), multiplier ),
					duration.getUnit(),
					duration.getExpressionType()
			);
		}
		else {
			return new BinaryArithmeticExpression(
					left,
					BinaryArithmeticOperator.MULTIPLY,
					multiplier,
					(BasicValuedMapping) left.getExpressionType()
			);
		}
	}

	static Expression castToTimestamp(SqlAstNode node, SqmToSqlAstConverter converter) {
		final BasicType<?> nodeType = (BasicType<?>) ((Expression) node).getExpressionType().getSingleJdbcMapping();
		final FunctionRenderer cast = (FunctionRenderer)
				converter.getCreationContext().getSqmFunctionRegistry().findFunctionDescriptor( "cast" );
		final BasicType<?> timestampType =
				converter.getCreationContext().getTypeConfiguration()
						.getBasicTypeForJavaType( Timestamp.class );
		return new SelfRenderingFunctionSqlAstExpression(
				"cast",
				cast,
				List.of( node, new CastTarget( timestampType ) ),
				nodeType,
				nodeType
		);
	}

	protected static class NumberSeriesQueryTransformer implements QueryTransformer {

		protected final FunctionTableGroup functionTableGroup;
		protected final TableGroup targetTableGroup;
		protected final String positionColumnName;
		protected final boolean coerceToTimestamp;

		public NumberSeriesQueryTransformer(FunctionTableGroup functionTableGroup, TableGroup targetTableGroup, String positionColumnName, boolean coerceToTimestamp) {
			this.functionTableGroup = functionTableGroup;
			this.targetTableGroup = targetTableGroup;
			this.positionColumnName = positionColumnName;
			this.coerceToTimestamp = coerceToTimestamp;
		}

		@Override
		public QuerySpec transform(CteContainer cteContainer, QuerySpec querySpec, SqmToSqlAstConverter converter) {
			//noinspection unchecked
			final List<SqlAstNode> arguments = (List<SqlAstNode>) functionTableGroup.getPrimaryTableReference()
					.getFunctionExpression()
					.getArguments();
			final JdbcType boundType = ((Expression) arguments.get( 0 )).getExpressionType().getSingleJdbcMapping().getJdbcType();
			final boolean castTimestamp = coerceToTimestamp
					&& (boundType.getDdlTypeCode() == SqlTypes.DATE || boundType.getDdlTypeCode() == SqlTypes.TIME);
			final Expression start = castTimestamp
					? castToTimestamp( arguments.get( 0 ), converter )
					: (Expression) arguments.get( 0 );
			final Expression stop = castTimestamp
					? castToTimestamp( arguments.get( 1 ), converter )
					: (Expression) arguments.get( 1 );
			final Expression explicitStep = arguments.size() > 2
					? (Expression) arguments.get( 2 )
					: null;

			final TableGroup parentTableGroup = querySpec.getFromClause().queryTableGroups(
					tg -> tg.findTableGroupJoin( targetTableGroup ) == null ? null : tg
			);
			final PredicateContainer predicateContainer;
			if ( parentTableGroup != null ) {
				predicateContainer = parentTableGroup.findTableGroupJoin( targetTableGroup );
			}
			else {
				predicateContainer = querySpec;
			}
			final BasicType<Integer> integerType = converter.getSqmCreationContext()
					.getNodeBuilder()
					.getIntegerType();
			final Expression oneBasedOrdinal = new ColumnReference(
					functionTableGroup.getPrimaryTableReference().getIdentificationVariable(),
					positionColumnName,
					false,
					null,
					integerType
			);
			final Expression one = new QueryLiteral<>( 1, integerType );
			final Expression zeroBasedOrdinal = new BinaryArithmeticExpression(
					oneBasedOrdinal,
					BinaryArithmeticOperator.SUBTRACT,
					one,
					integerType
			);
			final Expression stepExpression = explicitStep != null
					? multiply( explicitStep, zeroBasedOrdinal )
					: zeroBasedOrdinal;
			final Expression nextValue = add( start, stepExpression, converter );

			// Add a predicate to ensure the current value is valid
			if ( explicitStep == null ) {
				// The default step is 1, so just check if value <= stop
				predicateContainer.applyPredicate(
						new ComparisonPredicate(
								nextValue,
								ComparisonOperator.LESS_THAN_OR_EQUAL,
								stop
						)
				);
			}
			else {
				// When start < stop, step must be positive and value is only valid if it's less than or equal to stop
				final BasicType<Boolean> booleanType = converter.getSqmCreationContext()
						.getNodeBuilder()
						.getBooleanType();
				final Predicate positiveProgress = new Junction(
						Junction.Nature.CONJUNCTION,
						List.of(
								new ComparisonPredicate(
										start,
										ComparisonOperator.LESS_THAN,
										stop
								),
								new ComparisonPredicate(
										explicitStep,
										ComparisonOperator.GREATER_THAN,
										multiply( explicitStep, -1, integerType )
								),
								new ComparisonPredicate(
										nextValue,
										ComparisonOperator.LESS_THAN_OR_EQUAL,
										stop
								)
						),
						booleanType
				);
				// When start > stop, step must be negative and value is only valid if it's greater than or equal to stop
				final Predicate negativeProgress = new Junction(
						Junction.Nature.CONJUNCTION,
						List.of(
								new ComparisonPredicate(
										start,
										ComparisonOperator.GREATER_THAN,
										stop
								),
								new ComparisonPredicate(
										explicitStep,
										ComparisonOperator.LESS_THAN,
										multiply( explicitStep, -1, integerType )
								),
								new ComparisonPredicate(
										nextValue,
										ComparisonOperator.GREATER_THAN_OR_EQUAL,
										stop
								)
						),
						booleanType
				);
				final Predicate initialValue = new Junction(
						Junction.Nature.CONJUNCTION,
						List.of(
								new ComparisonPredicate(
										start,
										ComparisonOperator.EQUAL,
										stop
								),
								new ComparisonPredicate(
										oneBasedOrdinal,
										ComparisonOperator.EQUAL,
										one
								)
						),
						booleanType
				);
				predicateContainer.applyPredicate(
						new Junction(
								Junction.Nature.DISJUNCTION,
								List.of( positiveProgress, negativeProgress, initialValue ),
								booleanType
						)
				);
			}

			return querySpec;
		}
	}

	protected static class NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver extends GenerateSeriesSetReturningFunctionTypeResolver {

		public NumberSeriesGenerateSeriesSetReturningFunctionTypeResolver(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression) {
			super( defaultValueColumnName, defaultIndexSelectionExpression );
		}

		protected SelectableMapping[] resolveIterationVariableBasedFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final Expression start = (Expression) arguments.get( 0 );
			final Expression stop = (Expression) arguments.get( 0 );
			final JdbcMappingContainer expressionType = NullnessHelper.coalesce(
					start.getExpressionType(),
					stop.getExpressionType()
			);
			final Expression explicitStep = arguments.size() > 2
					? (Expression) arguments.get( 2 )
					: null;
			final JdbcMapping type = expressionType.getSingleJdbcMapping();
			if ( type == null ) {
				throw new IllegalArgumentException(
						"Couldn't determine types of arguments to function 'generate_series'" );
			}

			final SelectableMapping indexMapping = withOrdinality ? new SelectableMappingImpl(
					"",
					defaultIndexSelectionExpression,
					new SelectablePath( CollectionPart.Nature.INDEX.getName() ),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					false,
					false,
					false,
					false,
					false,
					false,
					converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class )
			) : null;

			//t.x+(1*(s.x-1))
			final String startExpression = getStartExpression( start, tableIdentifierVariable, converter );
			final String stepExpression = getStepExpression( explicitStep, tableIdentifierVariable, converter );
			final String customReadExpression;
			if ( type.getJdbcType().isTemporal() ) {
				final org.hibernate.sql.ast.tree.expression.Duration step = (org.hibernate.sql.ast.tree.expression.Duration) explicitStep;
				customReadExpression = timestampadd( startExpression, stepExpression, type, step, converter );
			}
			else {
				customReadExpression = startExpression + "+" + stepExpression;
			}
			final String elementSelectionExpression = defaultValueColumnName == null
					? tableIdentifierVariable
					: defaultValueColumnName;
			final SelectableMapping elementMapping;
			if ( expressionType instanceof SqlTypedMapping typedMapping ) {
				elementMapping = new SelectableMappingImpl(
						"",
						elementSelectionExpression,
						new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
						customReadExpression,
						null,
						typedMapping.getColumnDefinition(),
						typedMapping.getLength(),
						typedMapping.getArrayLength(),
						typedMapping.getPrecision(),
						typedMapping.getScale(),
						typedMapping.getTemporalPrecision(),
						typedMapping.isLob(),
						true,
						false,
						false,
						false,
						false,
						type
				);
			}
			else {
				elementMapping = new SelectableMappingImpl(
						"",
						elementSelectionExpression,
						new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
						customReadExpression,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						false,
						true,
						false,
						false,
						false,
						false,
						type
				);
			}
			final SelectableMapping[] returnType;
			if ( indexMapping == null ) {
				returnType = new SelectableMapping[] {elementMapping};
			}
			else {
				returnType = new SelectableMapping[] {elementMapping, indexMapping};
			}
			return returnType;
		}

		private static String timestampadd(String startExpression, String stepExpression, JdbcMapping type, org.hibernate.sql.ast.tree.expression.Duration duration, SqmToSqlAstConverter converter) {
			final SqlAstCreationContext creationContext = converter.getCreationContext();

			final FunctionRenderer renderer = (FunctionRenderer) creationContext.getSqmFunctionRegistry()
					.findFunctionDescriptor( "timestampadd" );
			final QuerySpec fakeQuery = new QuerySpec( true );
			fakeQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl(
					new SelfRenderingFunctionSqlAstExpression(
							"timestampadd",
							renderer,
							List.of(
									new DurationUnit( duration.getUnit(), duration.getExpressionType() ),
									new SelfRenderingSqlFragmentExpression( stepExpression, duration.getExpressionType() ),
									new SelfRenderingSqlFragmentExpression( startExpression, type )
							),
							(ReturnableType<?>) type,
							type
					)
			) );
			final SqlAstTranslator<JdbcOperationQuerySelect> translator =
					creationContext.getDialect().getSqlAstTranslatorFactory()
							.buildSelectTranslator( creationContext.getSessionFactory(), new SelectStatement( fakeQuery ) );
			final JdbcOperationQuerySelect operation = translator.translate( null, QueryOptions.NONE );
			final String sqlString = operation.getSqlString();
			assert sqlString.startsWith( "select " );

			final int startIndex = "select ".length();
			final int fromIndex = sqlString.lastIndexOf( " from" );
			return fromIndex == -1
					? sqlString.substring( startIndex )
					: sqlString.substring( startIndex, fromIndex );
		}

		private String getStartExpression(Expression expression, String tableIdentifierVariable, SqmToSqlAstConverter walker) {
			return getExpression( expression, tableIdentifierVariable, "b", walker );
		}

		private String getStepExpression(@Nullable Expression explicitStep, String tableIdentifierVariable, SqmToSqlAstConverter walker) {
			if ( explicitStep == null ) {
				return "(" + Template.TEMPLATE + "." + defaultIndexSelectionExpression + "-1)";
			}
			else {
				return "(" + getExpression( explicitStep, tableIdentifierVariable, "s", walker ) + "*(" + Template.TEMPLATE + "." + defaultIndexSelectionExpression + "-1))";
			}
		}

		private String getExpression(Expression expression, String tableIdentifierVariable, String syntheticColumnName, SqmToSqlAstConverter walker) {
			if ( expression instanceof Literal literal ) {
				final SqlAstCreationContext creationContext = walker.getCreationContext();
				//noinspection unchecked
				return literal.getJdbcMapping().getJdbcLiteralFormatter()
						.toJdbcLiteral( literal.getLiteralValue(), creationContext.getDialect(),
								creationContext.getWrapperOptions() );
			}
			else if ( expression instanceof ColumnReference columnReference ) {
				return columnReference.getExpressionText();
			}
			else {
				return tableIdentifierVariable + "_." + syntheticColumnName;
			}
		}
	}
}
