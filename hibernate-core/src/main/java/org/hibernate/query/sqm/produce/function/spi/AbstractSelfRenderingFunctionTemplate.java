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
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelfRenderingFunctionTemplate extends AbstractSqmFunctionTemplate {
	private final String name;

	public AbstractSelfRenderingFunctionTemplate(
			String name,
			FunctionReturnTypeResolver returnTypeResolver,
			ArgumentsValidator argumentsValidator) {
		super( argumentsValidator, returnTypeResolver );
		this.name = name;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> resolvedReturnType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		//noinspection unchecked
		return new SelfRenderingSqmFunction(
				getRenderingFunctionSupport( arguments, resolvedReturnType, queryEngine ),
				arguments,
				resolvedReturnType,
				queryEngine.getCriteriaBuilder(),
				name
		);
	}

	protected abstract SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<?> resolvedReturnType,
			QueryEngine queryEngine);
}
