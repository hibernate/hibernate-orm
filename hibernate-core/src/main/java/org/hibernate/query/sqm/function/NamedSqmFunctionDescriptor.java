/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.Locale;

import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionDescriptor extends AbstractSqmFunctionDescriptor {
	private final String functionName;
	private final FunctionRenderingSupport renderingSupport;

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator) {
		this(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				(impliedTypeAccess, arguments) -> impliedTypeAccess.get()
		);
	}

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( argumentsValidator, returnTypeResolver );

		this.functionName = functionName;
		this.renderingSupport = new StandardFunctionRenderingSupport( useParenthesesWhenNoArgs );
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	protected String resolveFunctionName(String functionName) {
		return this.functionName;
	}

	@Override
	public FunctionRenderingSupport getRenderingSupport() {
		return renderingSupport;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"NamedSqmFunctionTemplate(%s)",
				functionName
		);
	}

}
