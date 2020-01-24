/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

import org.hibernate.query.sqm.function.VarArgsFunctionDescriptor;

/**
 * @author Christian Beikov
 */
public class VarArgsFunctionDescriptorBuilder {

	private final SqmFunctionRegistry registry;

	private final String begin;
	private final String sep;
	private final String end;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	public VarArgsFunctionDescriptorBuilder(SqmFunctionRegistry registry, String begin, String sep, String end) {
		this.registry = registry;
		this.begin = begin;
		this.sep = sep;
		this.end = end;
	}

	public VarArgsFunctionDescriptorBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public VarArgsFunctionDescriptorBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public VarArgsFunctionDescriptorBuilder setMinArgumentCount(int min) {
		return setArgumentsValidator( StandardArgumentsValidators.min( min ) );
	}

	public VarArgsFunctionDescriptorBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public VarArgsFunctionDescriptorBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public VarArgsFunctionDescriptorBuilder setInvariantType(BasicValuedMapping invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public SqmFunctionDescriptor register(String registrationKey) {
		return registry.register(
				registrationKey,
				new VarArgsFunctionDescriptor(
						begin,
						sep,
						end,
						argumentsValidator,
						returnTypeResolver
				)
		);
	}
}
