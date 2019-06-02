/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;

import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class NamedFunctionTemplateBuilder {

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String functionName;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	private boolean useParenthesesWhenNoArgs = true;
	private String argumentListSignature;

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

	public NamedFunctionTemplateBuilder setMinArgumentCount(int min) {
		return setArgumentsValidator( StandardArgumentsValidators.min( min ) );
	}

	public NamedFunctionTemplateBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public NamedFunctionTemplateBuilder setInvariantType(StandardBasicTypes.StandardBasicType<?> invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public NamedFunctionTemplateBuilder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		return this;
	}

	public NamedFunctionTemplateBuilder setArgumentListSignature(String argumentListSignature) {
		this.argumentListSignature = argumentListSignature;
		return this;
	}

	public SqmFunctionTemplate register() {
		return registry.register( registrationKey, template() );
	}

	public SqmFunctionTemplate template() {
		return new NamedSqmFunctionTemplate(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				registrationKey,
				argumentListSignature
		);
	}
}
