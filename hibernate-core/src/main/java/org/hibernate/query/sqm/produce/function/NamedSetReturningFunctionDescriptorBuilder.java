/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.function.NamedSqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;

/**
 * @since 7.0
 */
@Incubating
public class NamedSetReturningFunctionDescriptorBuilder {

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String functionName;
	private final SetReturningFunctionTypeResolver setReturningTypeResolver;

	private ArgumentsValidator argumentsValidator;
	private FunctionArgumentTypeResolver argumentTypeResolver;

	private String argumentListSignature;
	private SqlAstNodeRenderingMode argumentRenderingMode = SqlAstNodeRenderingMode.DEFAULT;
	private boolean rendersColumnNames;

	public NamedSetReturningFunctionDescriptorBuilder(
			SqmFunctionRegistry registry,
			String registrationKey,
			String functionName,
			SetReturningFunctionTypeResolver typeResolver) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.functionName = functionName;
		this.setReturningTypeResolver = typeResolver;
	}

	public NamedSetReturningFunctionDescriptorBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public NamedSetReturningFunctionDescriptorBuilder setArgumentTypeResolver(FunctionArgumentTypeResolver argumentTypeResolver) {
		this.argumentTypeResolver = argumentTypeResolver;
		return this;
	}

	public NamedSetReturningFunctionDescriptorBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public NamedSetReturningFunctionDescriptorBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public NamedSetReturningFunctionDescriptorBuilder setMinArgumentCount(int min) {
		return setArgumentsValidator( StandardArgumentsValidators.min( min ) );
	}

	public NamedSetReturningFunctionDescriptorBuilder setParameterTypes(FunctionParameterType... types) {
		setArgumentsValidator( new ArgumentTypesValidator(argumentsValidator, types) );
		setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.invariant( types ) );
		return this;
	}

	public NamedSetReturningFunctionDescriptorBuilder setArgumentListSignature(String argumentListSignature) {
		this.argumentListSignature = argumentListSignature;
		return this;
	}

	public NamedSetReturningFunctionDescriptorBuilder setArgumentRenderingMode(SqlAstNodeRenderingMode argumentRenderingMode) {
		this.argumentRenderingMode = argumentRenderingMode;
		return this;
	}

	public NamedSetReturningFunctionDescriptorBuilder setRendersColumnNames(boolean rendersColumnNames) {
		this.rendersColumnNames = rendersColumnNames;
		return this;
	}

	public SqmSetReturningFunctionDescriptor register() {
		return registry.register( registrationKey, descriptor() );
	}

	public SqmSetReturningFunctionDescriptor descriptor() {
		return new NamedSqmSetReturningFunctionDescriptor(
				functionName,
				argumentsValidator,
				setReturningTypeResolver,
				rendersColumnNames,
				argumentTypeResolver,
				registrationKey,
				argumentListSignature,
				argumentRenderingMode
		);
	}

}
