/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.spi.FunctionAsExpressionTemplate;

import org.jboss.logging.Logger;

/**
 * @author Christian Beikov
 */
public class VarArgsFunctionTemplateBuilder {
	private static final Logger log = Logger.getLogger( VarArgsFunctionTemplateBuilder.class );

	private final SqmFunctionRegistry registry;
	private final String registrationKey;

	private final String begin;
	private final String sep;
	private final String end;

	private ArgumentsValidator argumentsValidator;
	private FunctionReturnTypeResolver returnTypeResolver;

	public VarArgsFunctionTemplateBuilder(SqmFunctionRegistry registry, String registrationKey, String begin, String sep, String end) {
		this.registry = registry;
		this.registrationKey = registrationKey;
		this.begin = begin;
		this.sep = sep;
		this.end = end;
	}

	public VarArgsFunctionTemplateBuilder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator;
		return this;
	}

	public VarArgsFunctionTemplateBuilder setArgumentCountBetween(int min, int max) {
		return setArgumentsValidator( StandardArgumentsValidators.between( min, max ) );
	}

	public VarArgsFunctionTemplateBuilder setMinArgumentCount(int min) {
		return setArgumentsValidator( StandardArgumentsValidators.min( min ) );
	}

	public VarArgsFunctionTemplateBuilder setExactArgumentCount(int exactArgumentCount) {
		return setArgumentsValidator( StandardArgumentsValidators.exactly( exactArgumentCount ) );
	}

	public VarArgsFunctionTemplateBuilder setReturnTypeResolver(FunctionReturnTypeResolver returnTypeResolver) {
		this.returnTypeResolver = returnTypeResolver;
		return this;
	}

	public VarArgsFunctionTemplateBuilder setInvariantType(AllowableFunctionReturnType invariantType) {
		setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( invariantType ) );
		return this;
	}

	public SqmFunctionTemplate register() {
		return registry.register(
				registrationKey,
				new FunctionAsExpressionTemplate(
						begin,
						sep,
						end,
						returnTypeResolver,
						argumentsValidator
				)
		);
	}
}
