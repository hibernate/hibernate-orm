/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelfRenderingFunctionTemplate extends AbstractSqmFunctionTemplate {
	private final AllowableFunctionReturnType invariantReturnType;

	public AbstractSelfRenderingFunctionTemplate(
			AllowableFunctionReturnType invariantReturnType,
			ArgumentsValidator argumentsValidator) {
		super( argumentsValidator );
		this.invariantReturnType = invariantReturnType;
	}

	public AbstractSelfRenderingFunctionTemplate(AllowableFunctionReturnType invariantReturnType) {
		super();
		this.invariantReturnType = invariantReturnType;
	}

	public AbstractSelfRenderingFunctionTemplate(ArgumentsValidator argumentsValidator) {
		super( argumentsValidator );
		this.invariantReturnType = null;
	}

	/**
	 * Get the invariant return type registered with the template when it was
	 * created.  If the function does not have an invariant type, returns
	 * {@code null}
	 */
	public AllowableFunctionReturnType getInvariantReturnType() {
		return invariantReturnType;
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new SelfRenderingSqmFunction(
				getRenderingFunctionSupport( arguments, impliedResultType ),
				arguments,
				impliedResultType
		);
	}

	protected abstract SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType);
}
