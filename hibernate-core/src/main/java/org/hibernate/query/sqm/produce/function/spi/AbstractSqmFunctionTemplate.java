/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionTemplate implements SqmFunctionTemplate {
	private final ArgumentsValidator argumentsValidator;

	public AbstractSqmFunctionTemplate() {
		this( null );
	}

	public AbstractSqmFunctionTemplate(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
	}


	@Override
	public final SqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		if ( argumentsValidator != null ) {
			argumentsValidator.validate( arguments );
		}

		return generateSqmFunctionExpression( arguments, impliedResultType );
	}

	protected abstract SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType);
}
