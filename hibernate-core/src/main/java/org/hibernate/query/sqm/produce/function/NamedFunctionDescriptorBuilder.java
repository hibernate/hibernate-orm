/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class NamedFunctionDescriptorBuilder {

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String functionName;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	private boolean useParenthesesWhenNoArgs = true;
	private String argumentListSignature;

	public NamedFunctionDescriptorBuilder(SqmFunctionRegistry registry, String registrationKey, String functionName) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.functionName = functionName;
	}

	public NamedFunctionDescriptorBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public NamedFunctionDescriptorBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public NamedFunctionDescriptorBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public NamedFunctionDescriptorBuilder setMinArgumentCount(int min) {
		return setArgumentsValidator( StandardArgumentsValidators.min( min ) );
	}

	public NamedFunctionDescriptorBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public NamedFunctionDescriptorBuilder setInvariantType(BasicType invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public NamedFunctionDescriptorBuilder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		return this;
	}

	public NamedFunctionDescriptorBuilder setArgumentListSignature(String argumentListSignature) {
		this.argumentListSignature = argumentListSignature;
		return this;
	}

	public SqmFunctionDescriptor register() {
		return registry.register( registrationKey, template() );
	}

	public SqmFunctionDescriptor template() {
		return new NamedSqmFunctionDescriptor(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				registrationKey,
				argumentListSignature
		);
	}
}
