/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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

	public AbstractSqmFunctionTemplate(FunctionReturnTypeResolver returnTypeResolver) {
		this( null, returnTypeResolver );
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
	public final SqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		argumentsValidator.validate( arguments );

		return generateSqmFunctionExpression(
				arguments,
				returnTypeResolver.resolveFunctionReturnType( impliedResultType, arguments )
		);
	}

	protected abstract SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType);
}
