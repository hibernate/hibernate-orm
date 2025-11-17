/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class NamedFunctionDescriptorBuilder {

	private final SqmFunctionRegistry registry;
	private final String registrationKey;
	private final FunctionKind functionKind;

	private final String functionName;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;
	private FunctionArgumentTypeResolver argumentTypeResolver;

	private boolean useParenthesesWhenNoArgs = true;
	private String argumentListSignature;
	private SqlAstNodeRenderingMode argumentRenderingMode = SqlAstNodeRenderingMode.DEFAULT;

	public NamedFunctionDescriptorBuilder(
			SqmFunctionRegistry registry,
			String registrationKey,
			FunctionKind functionKind,
			String functionName) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.functionKind = functionKind;
		this.functionName = functionName;
	}

	public NamedFunctionDescriptorBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public NamedFunctionDescriptorBuilder setArgumentTypeResolver(FunctionArgumentTypeResolver argumentTypeResolver) {
		this.argumentTypeResolver = argumentTypeResolver;
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

	public NamedFunctionDescriptorBuilder setInvariantType(BasicType<?> invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public NamedFunctionDescriptorBuilder setParameterTypes(FunctionParameterType... types) {
		setArgumentsValidator( new ArgumentTypesValidator(argumentsValidator, types) );
		setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.invariant( types ) );
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

	public NamedFunctionDescriptorBuilder setArgumentRenderingMode(SqlAstNodeRenderingMode argumentRenderingMode) {
		this.argumentRenderingMode = argumentRenderingMode;
		return this;
	}

	public SqmFunctionDescriptor register() {
		return registry.register( registrationKey, descriptor() );
	}

	public SqmFunctionDescriptor descriptor() {
		return new NamedSqmFunctionDescriptor(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				argumentTypeResolver,
				registrationKey,
				functionKind,
				argumentListSignature,
				argumentRenderingMode
		);
	}

}
