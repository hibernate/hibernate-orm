/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.List;

/**
 * @author Gavin King
 */
public class LocateEmulationFunction
		extends AbstractSqmFunctionTemplate {

	private SqmFunctionTemplate binaryFunction;
	private SqmFunctionTemplate ternaryFunction;

	public LocateEmulationFunction(
			SqmFunctionTemplate binaryFunction,
			SqmFunctionTemplate ternaryFunction) {
		super( StandardArgumentsValidators.between( 2, 3 ) );
		this.binaryFunction = binaryFunction;
		this.ternaryFunction = ternaryFunction;
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return ( arguments.size()<3 ? binaryFunction : ternaryFunction )
				.makeSqmFunctionExpression( arguments, impliedResultType );
	}
}
