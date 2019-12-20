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
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.function.PatternBasedSqmFunctionTemplate;

/**
 * @author Steve Ebersole
 */
public class PatternFunctionDescriptorBuilder {
	private final SqmFunctionRegistry registry;
	private final String registrationKey;
	private final String pattern;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	private boolean useParenthesesWhenNoArgs;

	public PatternFunctionDescriptorBuilder(SqmFunctionRegistry registry, String registrationKey, String pattern) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.pattern = pattern;
	}

	public PatternFunctionDescriptorBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public PatternFunctionDescriptorBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public PatternFunctionDescriptorBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public PatternFunctionDescriptorBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public PatternFunctionDescriptorBuilder setInvariantType(AllowableFunctionReturnType invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public PatternFunctionDescriptorBuilder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		return this;
	}

	public SqmFunctionDescriptor register() {
		return registry.register( registrationKey, template() );
	}

	public SqmFunctionDescriptor template() {
		return new PatternBasedSqmFunctionTemplate(
				new PatternRenderer( pattern, useParenthesesWhenNoArgs ),
				argumentsValidator,
				returnTypeResolver
		);
	}
}
