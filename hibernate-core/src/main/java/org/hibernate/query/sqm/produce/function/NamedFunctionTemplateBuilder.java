/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NamedFunctionTemplateBuilder {
	private static final Logger log = Logger.getLogger( NamedFunctionTemplateBuilder.class );

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String functionName;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	private boolean useParenthesesWhenNoArgs;

	public NamedFunctionTemplateBuilder(SqmFunctionRegistry registry, String functionName) {
		this.registry = registry;
		this.registrationKey = functionName;
		this.functionName = functionName;

	}

	public NamedFunctionTemplateBuilder(SqmFunctionRegistry registry, String registrationKey, String functionName) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.functionName = functionName;
	}

	public NamedFunctionTemplateBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public NamedFunctionTemplateBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public NamedFunctionTemplateBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public NamedFunctionTemplateBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public NamedFunctionTemplateBuilder setInvariantType(AllowableFunctionReturnType invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public NamedFunctionTemplateBuilder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		return this;
	}

	public SqmFunctionTemplate register() {
		return registry.register(
				registrationKey,
				new NamedSqmFunctionTemplate(
						functionName,
						useParenthesesWhenNoArgs,
						argumentsValidator,
						returnTypeResolver
				)
		);
	}
}
