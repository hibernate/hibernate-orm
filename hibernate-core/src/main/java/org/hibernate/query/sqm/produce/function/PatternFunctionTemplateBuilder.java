/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.produce.function.spi.PatternBasedSqmFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class PatternFunctionTemplateBuilder {
	private final SqmFunctionRegistry registry;
	private final String registrationKey;
	private final String pattern;
	private String argumentListSignature;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	public PatternFunctionTemplateBuilder(SqmFunctionRegistry registry, String registrationKey, String pattern) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.pattern = pattern;
	}

	public PatternFunctionTemplateBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public PatternFunctionTemplateBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public PatternFunctionTemplateBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public PatternFunctionTemplateBuilder setInvariantType(StandardBasicTypes.StandardBasicType<?> invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public PatternFunctionTemplateBuilder setArgumentListSignature(String argumentListSignature) {
		this.argumentListSignature = argumentListSignature;
		return this;
	}

	public SqmFunctionTemplate register() {
		return registry.register( registrationKey, template() );
	}

	public SqmFunctionTemplate template() {
		return new PatternBasedSqmFunctionTemplate(
				new PatternRenderer( pattern ),
				argumentsValidator,
				returnTypeResolver,
				registrationKey,
				argumentListSignature
		);
	}
}
