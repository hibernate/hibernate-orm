/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NamedFunctionDescriptorBuilder {
	private static final Logger log = Logger.getLogger( NamedFunctionDescriptorBuilder.class );

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String functionName;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	private boolean useParenthesesWhenNoArgs;

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

	public NamedFunctionDescriptorBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public NamedFunctionDescriptorBuilder setInvariantType(AllowableFunctionReturnType invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public NamedFunctionDescriptorBuilder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		return this;
	}

	public SqmFunctionDescriptor register() {
		return registry.register(
				registrationKey,
				build()
		);
	}

	public SqmFunctionDescriptor build() {
		return new NamedSqmFunctionDescriptor(
				registrationKey,
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator
		);
	}
}
