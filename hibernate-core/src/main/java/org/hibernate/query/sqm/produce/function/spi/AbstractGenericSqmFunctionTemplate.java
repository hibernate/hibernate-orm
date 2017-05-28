/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;

/**
 * SqmFunctionTemplate implementation support for basic implementations
 * generating a {@link SqmGenericFunction}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGenericSqmFunctionTemplate implements SqmFunctionTemplate {
	private final String name;

	public AbstractGenericSqmFunctionTemplate(String name) {
		this.name = name;
	}

	@Override
	public SqmFunction makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new SqmGenericFunction(
				name,
				impliedResultType,
				arguments
		);
	}
}
