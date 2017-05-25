/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.FunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.GenericFunctionSqmExpression;

/**
 * SQLFunction implementation support for impls generating a
 * {@link GenericFunctionSqmExpression}
 *
 * @author Steve Ebersole
 */
public abstract class GenericSqmFunctionTemplateSupport implements SqmFunctionTemplate {
	private final String name;

	public GenericSqmFunctionTemplateSupport(String name) {
		this.name = name;
	}

	@Override
	public FunctionSqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new GenericFunctionSqmExpression(
				name,
				impliedResultType,
				arguments
		);
	}
}
