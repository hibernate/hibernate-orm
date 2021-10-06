/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import jakarta.persistence.criteria.Expression;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class InsertSubstringOverlayEmulation
		extends AbstractSqmFunctionDescriptor {

	private final boolean strictSubstring;

	public InsertSubstringOverlayEmulation(TypeConfiguration typeConfiguration, boolean strictSubstring) {
		super(
				"overlay",
				StandardArgumentsValidators.between( 3, 4 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
		this.strictSubstring = strictSubstring;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final BasicType<Integer> intType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		final BasicType<String> stringType = typeConfiguration.getBasicTypeForJavaType( String.class );

		SqmTypedNode<?> string = arguments.get(0);
		SqmTypedNode<?> replacement = arguments.get(1);
		SqmTypedNode<?> start = arguments.get(2);
		SqmTypedNode<?> length = arguments.size() > 3
				? arguments.get(3)
				: queryEngine.getSqmFunctionRegistry().findFunctionDescriptor("length")
						.generateSqmExpression( replacement, intType, queryEngine, typeConfiguration );

		SqmFunctionDescriptor insert = queryEngine.getSqmFunctionRegistry().findFunctionDescriptor("insert");
		if ( insert != null ) {
			return insert.generateSqmExpression(
					asList( string, start, length, replacement ),
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
		}
		else {
			SqmFunctionDescriptor lengthFunction = queryEngine.getSqmFunctionRegistry().findFunctionDescriptor("length");
			SqmFunctionDescriptor substring = queryEngine.getSqmFunctionRegistry().findFunctionDescriptor("substring");
			SqmFunctionDescriptor concat = queryEngine.getSqmFunctionRegistry().findFunctionDescriptor("concat");
			SqmLiteral<Integer> one = new SqmLiteral<>( 1, intType, queryEngine.getCriteriaBuilder() );
			SqmExpression<Integer> startPlusLength = new SqmBinaryArithmetic<>(
					BinaryArithmeticOperator.ADD,
					(SqmExpression<?>) start,
					(SqmExpression<?>) length,
					intType,
					queryEngine.getCriteriaBuilder()
			);
			SqmExpression<Integer> startMinusOne = new SqmBinaryArithmetic<>(
					BinaryArithmeticOperator.SUBTRACT,
					(SqmExpression<?>) start,
					one,
					intType,
					queryEngine.getCriteriaBuilder()
			);
			SqmTypedNode<?> restString = substring.generateSqmExpression(
					asList( string, startPlusLength ),
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
			if ( strictSubstring ) {
				restString = new SqmCaseSearched<>( stringType, start.nodeBuilder() )
						.when(
								new SqmComparisonPredicate(
										startPlusLength,
										ComparisonOperator.GREATER_THAN,
										lengthFunction.generateSqmExpression(
												asList( string ),
												intType,
												queryEngine,
												typeConfiguration
										),
										string.nodeBuilder()
								),
								new SqmLiteral<>( "", stringType, string.nodeBuilder() )
						).otherwise( (Expression<? extends String>) restString );
			}
			return concat.generateSqmExpression(
					asList(
							substring.generateSqmExpression(
									asList( string, one, startMinusOne ),
									impliedResultType,
									queryEngine,
									typeConfiguration
							),
							replacement,
							restString
					),
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(string placing replacement from start[ for length])";
	}
}
