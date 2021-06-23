/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.*;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.ZoneOffset;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.BinaryArithmeticOperator.*;
import static org.hibernate.query.TemporalUnit.*;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * @author Gavin King
 */
public class ExtractFunction
		extends AbstractSqmFunctionDescriptor {

	private final Dialect dialect;

	public ExtractFunction(Dialect dialect) {
		super(
				"extract",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 )
		);
		this.dialect = dialect;
	}

	@Override
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
			case NATIVE:
				throw new SemanticException("can't extract() the field TemporalUnit.NATIVE");
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
				String pattern = dialect.extractPattern( unit );
				return queryEngine.getSqmFunctionRegistry()
						.patternDescriptorBuilder( "extract", pattern )
						.setExactArgumentCount( 2 )
						.setReturnTypeResolver( useArgType( 1 ) )
						.descriptor()
						.generateSqmExpression(
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
		final NodeBuilder builder = field.nodeBuilder();

		final BasicType<Integer> intType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		final BasicType<Float> floatType = typeConfiguration.getBasicTypeForJavaType( Float.class );

		final SqmExtractUnit<Integer> dayOfUnit = new SqmExtractUnit<>( dayOf, intType, builder );
		final SqmExpression<Integer> extractDayOf
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionDescriptor("extract")
						.generateSqmExpression(
								asList( dayOfUnit, expressionToExtract ),
								intType,
								queryEngine,
								typeConfiguration
						);

		final SqmExtractUnit<Integer> dayOfWeekUnit = new SqmExtractUnit<>( DAY_OF_WEEK, intType, builder );
		final SqmExpression<Integer> extractDayOfWeek
				= queryEngine.getSqmFunctionRegistry()
						.findFunctionDescriptor("extract")
						.generateSqmExpression(
								asList( dayOfWeekUnit, expressionToExtract ),
								intType,
								queryEngine,
								typeConfiguration
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
							queryEngine,
							typeConfiguration
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
		BasicType<Long> longType = typeConfiguration.getBasicTypeForJavaType(Long.class);
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("floor")
				.generateSqmExpression(
						arg,
						longType, // Implicit cast to long
						queryEngine,
						typeConfiguration
				);
	}

	private SelfRenderingSqmFunction<Long> extractNanoseconds(
			SqmExpression<?> expressionToExtract,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

		final BasicType<Float> floatType = typeConfiguration.getBasicTypeForJavaType(Float.class);

		final SqmExtractUnit<Float> extractSeconds = new SqmExtractUnit<>( SECOND, floatType, builder );
		final SqmLiteral<Float> billion = new SqmLiteral<>( 1e9f, floatType, builder );
		return toLong(
				new SqmBinaryArithmetic<>(
						MULTIPLY,
						generateSqmExpression(
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
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

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
						queryEngine,
						typeConfiguration
				);
	}

	private SelfRenderingSqmFunction<?> extractDateOrTimeUsingCast(
			SqmExpression<?> expressionToExtract,
			AllowableFunctionReturnType<?> type,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final NodeBuilder builder = expressionToExtract.nodeBuilder();

		final SqmCastTarget<?> target = new SqmCastTarget<>( type, builder );
		return queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor("cast")
				.generateSqmExpression(
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
