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
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionTemplate implements SqmFunctionTemplate {
	private final ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;

	public AbstractSqmFunctionTemplate() {
		this( null, null );
	}

	public AbstractSqmFunctionTemplate(ArgumentsValidator argumentsValidator) {
		this( argumentsValidator, null );
	}

	public AbstractSqmFunctionTemplate(
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
		this.returnTypeResolver = returnTypeResolver == null
				? StandardFunctionReturnTypeResolvers.useFirstNonNull()
				: returnTypeResolver;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <T> SelfRenderingSqmFunction<T> makeSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		argumentsValidator.validate( arguments );

		return generateSqmFunctionExpression(
				arguments,
				(AllowableFunctionReturnType<T>) //this cast is not truly correct
						returnTypeResolver.resolveFunctionReturnType( impliedResultType, arguments ),
				queryEngine
		);
	}

	protected abstract <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine);
}
