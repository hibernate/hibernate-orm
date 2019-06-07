/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class InsertSubstringOverlayEmulation
		extends AbstractSqmFunctionTemplate {

	public InsertSubstringOverlayEmulation() {
		super(
				StandardArgumentsValidators.between( 3, 4 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		BasicValuedExpressableType<Integer> intType = typeConfiguration.standardExpressableTypeForJavaType(Integer.class);

		SqmTypedNode<?> string = arguments.get(0);
		SqmTypedNode<?> replacement = arguments.get(1);
		SqmTypedNode<?> start = arguments.get(2);
		SqmTypedNode<?> length = arguments.size() > 3
				? arguments.get(3)
				: queryEngine.getSqmFunctionRegistry().findFunctionTemplate("length")
						.makeSqmFunctionExpression( replacement, intType, queryEngine, typeConfiguration );

		SqmFunctionTemplate insert = queryEngine.getSqmFunctionRegistry().findFunctionTemplate("insert");
		if ( insert != null ) {
			return insert.makeSqmFunctionExpression(
					asList( string, start, length, replacement ),
					impliedResultType,
					queryEngine,
					typeConfiguration
			);
		}
		else {
			SqmFunctionTemplate substring = queryEngine.getSqmFunctionRegistry().findFunctionTemplate("substring");
			SqmFunctionTemplate concat = queryEngine.getSqmFunctionRegistry().findFunctionTemplate("concat");
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
			return concat.makeSqmFunctionExpression(
					asList(
							substring.makeSqmFunctionExpression(
									asList( string, one, startMinusOne ),
									impliedResultType,
									queryEngine,
									typeConfiguration
							),
							replacement,
							substring.makeSqmFunctionExpression(
									asList( string, startPlusLength ),
									impliedResultType,
									queryEngine,
									typeConfiguration
							)
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
