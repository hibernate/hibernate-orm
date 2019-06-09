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
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.sql.TrimSpec.LEADING;

/**
 * @author Gavin King
 */
public class LpadRpadPadEmulation
		extends AbstractSqmFunctionTemplate {

	public LpadRpadPadEmulation() {
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
		SqmTrimSpecification padSpec = (SqmTrimSpecification) arguments.get(2);
		String padName = padSpec.getSpecification() == LEADING ? "lpad" : "rpad";
		return queryEngine.getSqmFunctionRegistry().findFunctionTemplate( padName )
				.makeSqmFunctionExpression(
						arguments.size() > 3
						? asList(
								arguments.get(0),
								arguments.get(1),
								arguments.get(3)
						)
						: asList(
								arguments.get(0),
								arguments.get(1)
						),
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	@Override
	public String getArgumentListSignature() {
		return "(string with length {leading|trailing}[ character])";
	}
}
