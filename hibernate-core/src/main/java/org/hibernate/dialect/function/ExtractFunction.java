/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.*;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.*;
import static org.hibernate.query.common.TemporalUnit.*;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;
import static org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME;

/**
 * ANSI SQL-inspired {@code extract()} function, where the date/time fields
 * are enumerated by {@link TemporalUnit}, and portability is achieved
 * by delegating to {@link Dialect#extractPattern(TemporalUnit)}.
 *
 * @author Gavin King
 */
public class ExtractFunction extends AbstractSqmFunctionDescriptor implements FunctionRenderer {

	private final Dialect dialect;

	public ExtractFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super(
				"extract",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						TEMPORAL_UNIT, TEMPORAL
				),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, TEMPORAL )
		);
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final ExtractUnit field = (ExtractUnit) sqlAstArguments.get( 0 );
		final TemporalUnit unit = field.getUnit();
		final String pattern = dialect.extractPattern( unit );
		new PatternRenderer( pattern ).render( sqlAppender, sqlAstArguments, walker );
	}

	@Override
	protected <T> SelfRenderingSqmFunction generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get( 0 );
		final SqmExpression<?> originalExpression = (SqmExpression<?>) arguments.get( 1 );
		final boolean compositeTemporal = SqmExpressionHelper.isCompositeTemporal( originalExpression );
		final SqmExpression<?> expression = SqmExpressionHelper.getOffsetAdjustedExpression( originalExpression );

		TemporalUnit unit = field.getUnit();
		switch ( unit ) {
			case NANOSECOND:
				return extractNanoseconds( expression, queryEngine );
			case NATIVE:
				throw new SemanticException("NATIVE is not a legal field for extract()");
			case OFFSET:
				if ( compositeTemporal ) {
					final SqmPath<Object> offsetPath = ( (SqmPath<?>) originalExpression ).get(
							ZONE_OFFSET_NAME
					);
					return new SelfRenderingSqmFunction<>(
							this,
							(sqlAppender, sqlAstArguments, returnType, walker) -> sqlAstArguments.get( 0 ).accept( walker ),
							Collections.singletonList( offsetPath ),
							null,
							null,
							StandardFunctionReturnTypeResolvers.useArgType( 1 ),
							expression.nodeBuilder(),
							"extract"
					);
				}
				else {
					// use format(arg, 'xxx') to get the offset
					return extractOffsetUsingFormat( expression, queryEngine );
				}
			case DATE:
			case TIME:
				// use cast(arg as Type) to get the date or time part
				// which might be javax.sql.Date / javax.sql.Time or
				// java.time.LocalDate / java.time.LocalTime depending
				// on the type of the expression we're extracting from
				return extractDateOrTimeUsingCast( expression, field.getType(), queryEngine );
			case WEEK_OF_MONTH:
				// use ceiling(extract(day of month, arg)/7.0)
				return extractWeek( expression, field, DAY_OF_MONTH, queryEngine );
			case WEEK_OF_YEAR:
				// use ceiling(extract(day of year, arg)/7.0)
				return extractWeek( expression, field, DAY_OF_YEAR, queryEngine );
			default:
				// otherwise it's something we expect the SQL dialect
				// itself to understand, either natively, or via the
				// method Dialect.extract()
				return new SelfRenderingSqmFunction<>(
						this,
						this,
						expression == originalExpression ? arguments : List.of( arguments.get( 0 ), expression ),
						impliedResultType,
						getArgumentsValidator(),
						getReturnTypeResolver(),
						expression.nodeBuilder(),
						"extract"
				);
		}
	}

	private SelfRenderingSqmFunction<Integer> extractWeek(
			SqmExpression<?> expressionToExtract,
			SqmExtractUnit<?> field,
			TemporalUnit dayOf,
			QueryEngine queryEngine) {
		final NodeBuilder builder = field.nodeBuilder();

		final TypeConfiguration typeConfiguration = queryEngine.getTypeConfiguration();
		final BasicType<Integer> intType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		final BasicType<Float> floatType = typeConfiguration.getBasicTypeForJavaType( Float.class );

		final SqmExtractUnit<Integer> dayOfUnit = new SqmExtractUnit<>( dayOf, intType, builder );
		final SqmExpression<Integer> extractDayOf
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionDescriptor("extract")
						.generateSqmExpression(
								asList( dayOfUnit, expressionToExtract ),
								intType,
								queryEngine
						);

		final SqmExtractUnit<Integer> dayOfWeekUnit = new SqmExtractUnit<>( DAY_OF_WEEK, intType, builder );
		final SqmExpression<Integer> extractDayOfWeek
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionDescriptor("extract")
						.generateSqmExpression(
								asList( dayOfWeekUnit, expressionToExtract ),
								intType,
								queryEngine
						);

		final SqmLiteral<Float> seven = new SqmLiteral<>( 7.0f, floatType, builder );
		final SqmLiteral<Integer> one = new SqmLiteral<>( 1, intType, builder );
		final SqmBinaryArithmetic<Integer> daySubtractionInt = new SqmBinaryArithmetic<>(
				SUBTRACT,
				extractDayOf,
				extractDayOfWeek,
				intType,
				builder
		);
		final SqmExpression<?> daySubtraction;
		if ( dialect.requiresFloatCastingOfIntegerDivision() ) {
			daySubtraction = queryEngine.getSqmFunctionRegistry()
					.findFunctionDescriptor("cast")
					.generateSqmExpression(
							asList( daySubtractionInt, new SqmCastTarget<>( floatType, builder ) ),
							floatType,
							queryEngine
					);
		}
		else {
			daySubtraction = daySubtractionInt;
		}
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("ceiling")
				.generateSqmExpression(
						new SqmBinaryArithmetic<>(
								ADD,
								new SqmBinaryArithmetic<>(
										DIVIDE,
										daySubtraction,
										seven,
										floatType,
										builder
								),
								one,
								intType,
								builder
						),
						intType, // Implicit cast to int
						queryEngine
				);
	}

	private SelfRenderingSqmFunction<Long> toLong(
			SqmExpression<?> arg,
			QueryEngine queryEngine) {
		//Not every database supports round() (looking at you Derby)
		//so use floor() instead, which is perfectly fine for this
//		return getFunctionTemplate("round").makeSqmFunctionExpression(
//				asList( arg, integerLiteral("0") ),
//				basicType( Long.class ),
//				creationContext.getQueryEngine(),
//				creationContext.getDomainModel().getTypeConfiguration()
//		);
		BasicType<Long> longType = queryEngine.getTypeConfiguration().getBasicTypeForJavaType(Long.class);
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("floor")
				.generateSqmExpression(
						arg,
						longType, // Implicit cast to long
						queryEngine
				);
	}

	private SelfRenderingSqmFunction<Long> extractNanoseconds(
			SqmExpression<?> expressionToExtract,
			QueryEngine queryEngine) {
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

		final TypeConfiguration typeConfiguration = queryEngine.getTypeConfiguration();
		final BasicType<Float> floatType = typeConfiguration.getBasicTypeForJavaType(Float.class);

		final SqmExtractUnit<Float> extractSeconds = new SqmExtractUnit<>( SECOND, floatType, builder );
		final SqmLiteral<Float> billion = new SqmLiteral<>( 1e9f, floatType, builder );
		return toLong(
				new SqmBinaryArithmetic<>(
						MULTIPLY,
						generateSqmExpression(
								asList( extractSeconds, expressionToExtract ),
								floatType,
								queryEngine
						),
						billion,
						floatType,
						builder
				),
				queryEngine
		);
	}

	private SelfRenderingSqmFunction<ZoneOffset> extractOffsetUsingFormat(
			SqmExpression<?> expressionToExtract,
			QueryEngine queryEngine) {
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

		final TypeConfiguration typeConfiguration = queryEngine.getTypeConfiguration();
		final BasicType<ZoneOffset> offsetType = typeConfiguration.getBasicTypeForJavaType(ZoneOffset.class);
		final BasicType<String> stringType = typeConfiguration.getBasicTypeForJavaType(String.class);

		final SqmFormat offsetFormat = new SqmFormat(
				"xxx", //pattern for timezone offset
				stringType,
				builder
		);
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("format")
				.generateSqmExpression(
						asList( expressionToExtract, offsetFormat ),
						offsetType,
						queryEngine
				);
	}

	private SelfRenderingSqmFunction<?> extractDateOrTimeUsingCast(
			SqmExpression<?> expressionToExtract,
			ReturnableType<?> type,
			QueryEngine queryEngine) {
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

		final SqmCastTarget<?> target = new SqmCastTarget<>( type, builder );
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("cast")
				.generateSqmExpression(
						asList( expressionToExtract, target ),
						type,
						queryEngine
				);
	}


	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field from TEMPORAL arg)";
	}

}
