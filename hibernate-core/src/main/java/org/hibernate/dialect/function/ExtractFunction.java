/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.ZoneOffset;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.BinaryArithmeticOperator.DIVIDE;
import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.query.TemporalUnit.DAY_OF_MONTH;
import static org.hibernate.query.TemporalUnit.DAY_OF_WEEK;
import static org.hibernate.query.TemporalUnit.DAY_OF_YEAR;
import static org.hibernate.query.TemporalUnit.SECOND;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * @author Gavin King
 */
public class ExtractFunction
		extends AbstractSqmFunctionTemplate {

	private Dialect dialect;

	public ExtractFunction(Dialect dialect) {
		super(
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 )
		);
		this.dialect = dialect;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> SelfRenderingSqmFunction generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get(0);
		SqmExpression<?> expression = (SqmExpression<?>) arguments.get(1);

		TemporalUnit unit = field.getUnit();
		switch ( unit ) {
			case NANOSECOND:
				return extractNanoseconds( expression, queryEngine, typeConfiguration );
			case OFFSET:
				// use format(arg, 'xxx') to get the offset
				return extractOffsetUsingFormat( expression, queryEngine, typeConfiguration );
			case DATE:
			case TIME:
				// use cast(arg as Type) to get the date or time part
				// which might be javax.sql.Date / javax.sql.Time or
				// java.time.LocalDate / java.time.LocalTime depending
				// on the type of the expression we're extracting from
				return extractDateOrTimeUsingCast( expression, field.getType(), queryEngine, typeConfiguration );
			case WEEK_OF_MONTH:
				// use ceiling(extract(day of month, arg)/7.0)
				return extractWeek( expression, field, DAY_OF_MONTH, queryEngine, typeConfiguration);
			case WEEK_OF_YEAR:
				// use ceiling(extract(day of year, arg)/7.0)
				return extractWeek( expression, field, DAY_OF_YEAR, queryEngine, typeConfiguration);
			default:
				// otherwise it's something we expect the SQL dialect
				// itself to understand, either natively, or via the
				// method Dialect.extract()
				String pattern = dialect.extract( unit );
				return queryEngine.getSqmFunctionRegistry()
						.patternTemplateBuilder( "extract", pattern )
						.setExactArgumentCount( 2 )
						.setReturnTypeResolver( useArgType( 1 ) )
						.template()
						.makeSqmFunctionExpression(
								arguments,
								impliedResultType,
								queryEngine,
								typeConfiguration
						);
		}
	}

	private SelfRenderingSqmFunction<Integer> extractWeek(
			SqmExpression<?> expressionToExtract,
			SqmExtractUnit<?> field,
			TemporalUnit dayOf,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		NodeBuilder builder = field.nodeBuilder();

		BasicValuedExpressableType<Integer> intType = typeConfiguration.standardExpressableTypeForJavaType( Integer.class );
		BasicValuedExpressableType<Float> floatType = typeConfiguration.standardExpressableTypeForJavaType( Float.class );

		SqmExtractUnit<Integer> dayOfUnit = new SqmExtractUnit<>( dayOf, intType, builder );
		SqmExpression<Integer> extractDayOf
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionTemplate("extract")
						.makeSqmFunctionExpression(
								asList( dayOfUnit, expressionToExtract ),
								intType,
								queryEngine,
								typeConfiguration
						);

		SqmExtractUnit<Integer> dayOfWeekUnit = new SqmExtractUnit<>( DAY_OF_WEEK, intType, builder );
		SqmExpression<Integer> extractDayOfWeek
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionTemplate("extract")
						.makeSqmFunctionExpression(
								asList( dayOfWeekUnit, expressionToExtract ),
								intType,
								queryEngine,
								typeConfiguration
						);

		SqmLiteral<Float> seven = new SqmLiteral<>( 7.0f, floatType, builder );
		SqmLiteral<Integer> one = new SqmLiteral<>( 1, intType, builder );

		return queryEngine.getSqmFunctionRegistry()
				.findFunctionTemplate("ceiling")
				.makeSqmFunctionExpression(
						new SqmBinaryArithmetic<>(
								ADD,
								new SqmBinaryArithmetic<>(
										DIVIDE,
										new SqmBinaryArithmetic<>(
												SUBTRACT,
												extractDayOf,
												extractDayOfWeek,
												intType,
												builder
										),
										seven,
										floatType,
										builder
								),
								one,
								intType,
								builder
						),
						intType,
						queryEngine,
						typeConfiguration
				);
	}

	private SelfRenderingSqmFunction<Long> toLong(
			SqmExpression<?> arg,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		//Not every database supports round() (looking at you Derby)
		//so use floor() instead, which is perfectly fine for this
//		return getFunctionTemplate("round").makeSqmFunctionExpression(
//				asList( arg, integerLiteral("0") ),
//				basicType( Long.class ),
//				creationContext.getQueryEngine(),
//				creationContext.getDomainModel().getTypeConfiguration()
//		);
		BasicValuedExpressableType<Long> longType = typeConfiguration.standardExpressableTypeForJavaType(Long.class);
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionTemplate("floor")
				.makeSqmFunctionExpression(
						arg,
						longType,
						queryEngine,
						typeConfiguration
				);
	}

	private SelfRenderingSqmFunction<Long> extractNanoseconds(
			SqmExpression<?> expressionToExtract,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		NodeBuilder builder = expressionToExtract.nodeBuilder();

		BasicValuedExpressableType<Float> floatType = typeConfiguration.standardExpressableTypeForJavaType(Float.class);

		SqmExtractUnit<Float> extractSeconds = new SqmExtractUnit<>( SECOND, floatType, builder );
		SqmLiteral<Float> billion = new SqmLiteral<>( 1e9f, floatType, builder );
		return toLong(
				new SqmBinaryArithmetic<>(
						MULTIPLY,
						queryEngine.getSqmFunctionRegistry()
								.findFunctionTemplate("extract")
								.makeSqmFunctionExpression(
										asList( extractSeconds, expressionToExtract ),
										floatType,
										queryEngine,
										typeConfiguration
								),
						billion,
						floatType,
						builder
				),
				queryEngine,
				typeConfiguration
		);
	}

	private SelfRenderingSqmFunction<ZoneOffset> extractOffsetUsingFormat(
			SqmExpression<?> expressionToExtract,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		NodeBuilder builder = expressionToExtract.nodeBuilder();

		BasicValuedExpressableType<ZoneOffset> offsetType = typeConfiguration.standardExpressableTypeForJavaType(ZoneOffset.class);
		BasicValuedExpressableType<String> stringType = typeConfiguration.standardExpressableTypeForJavaType(String.class);

		SqmFormat offsetFormat = new SqmFormat(
				"xxx", //pattern for timezone offset
				stringType,
				builder
		);
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionTemplate("format")
				.makeSqmFunctionExpression(
						asList( expressionToExtract, offsetFormat ),
						offsetType,
						queryEngine,
						typeConfiguration
				);
	}

	private SelfRenderingSqmFunction<?> extractDateOrTimeUsingCast(
			SqmExpression<?> expressionToExtract,
			AllowableFunctionReturnType<?> type,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		NodeBuilder builder = expressionToExtract.nodeBuilder();

		SqmCastTarget<?> target = new SqmCastTarget<>( type, builder );
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionTemplate("cast")
				.makeSqmFunctionExpression(
						asList( expressionToExtract, target ),
						type,
						queryEngine,
						typeConfiguration
				);
	}


	@Override
	public String getArgumentListSignature() {
		return "(field from arg)";
	}

}
