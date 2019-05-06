/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelfRenderingFunctionTemplate extends AbstractSqmFunctionTemplate {
	public AbstractSelfRenderingFunctionTemplate(
			FunctionReturnTypeResolver returnTypeResolver,
			ArgumentsValidator argumentsValidator) {
		super( argumentsValidator, returnTypeResolver );
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmTypedNode> arguments,
			AllowableFunctionReturnType resolvedReturnType,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return new SelfRenderingSqmFunction(
				getRenderingFunctionSupport( arguments, resolvedReturnType, queryEngine ),
				arguments,
				resolvedReturnType,
				queryEngine.getCriteriaBuilder()
		);
	}

	protected abstract SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmTypedNode> arguments,
			AllowableFunctionReturnType resolvedReturnType,
			QueryEngine queryEngine);
}
