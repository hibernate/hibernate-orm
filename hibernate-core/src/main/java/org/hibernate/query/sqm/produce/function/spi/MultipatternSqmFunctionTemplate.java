/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Support for overloaded functions defined in terms of a
 * list of patterns, one for each possible function arity.
 *
 * @see PatternBasedSqmFunctionTemplate
 *
 * @author Gavin King
 */
public class MultipatternSqmFunctionTemplate extends AbstractSqmFunctionTemplate {

	private SqmFunctionTemplate[] functions;
	private String argumentListSignature;

	private static int first(SqmFunctionTemplate[] functions) {
		for (int i=0; i<functions.length; i++) {
			if ( functions[i]!=null ) {
				return i;
			}
		}
		throw new IllegalArgumentException("no functions");
	}

	private static int last(SqmFunctionTemplate[] functions) {
		return functions.length-1;
	}

	/**
	 * Construct an instance with the given function templates
	 * where the position of each function template in the
	 * given array corresponds to the arity of the function
	 * template. The array must be padded with leading nulls
	 * where there is no overloaded form corresponding to
	 * lower arities.
	 *
	 * @param functions the function templates to delegate to,
	 *                  where array position corresponds to
	 *                  arity.
	 */
	public MultipatternSqmFunctionTemplate(
			SqmFunctionTemplate[] functions,
			FunctionReturnTypeResolver type) {
		super(
				StandardArgumentsValidators.between(
					first(functions),
					last(functions)
				),
				type
		);
		this.functions = functions;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return functions[ arguments.size() ]
				.makeSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature == null
				? super.getArgumentListSignature()
				: argumentListSignature;
	}

	public void setArgumentListSignature(String argumentListSignature) {
		this.argumentListSignature = argumentListSignature;
	}
}
