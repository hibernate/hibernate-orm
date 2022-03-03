/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Support for overloaded functions defined in terms of a
 * list of patterns, one for each possible function arity.
 *
 * @see PatternBasedSqmFunctionDescriptor
 *
 * @author Gavin King
 */
public class MultipatternSqmFunctionDescriptor extends AbstractSqmFunctionDescriptor {

	private SqmFunctionDescriptor[] functions;
	private String argumentListSignature;

	private static int first(SqmFunctionDescriptor[] functions) {
		for (int i=0; i<functions.length; i++) {
			if ( functions[i]!=null ) {
				return i;
			}
		}
		throw new IllegalArgumentException("no functions");
	}

	private static int last(SqmFunctionDescriptor[] functions) {
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
	 * @param name
	 * @param functions the function templates to delegate to,
	 * @param type
	 */
	public MultipatternSqmFunctionDescriptor(
			String name,
			SqmFunctionDescriptor[] functions,
			BasicType<?> type,
			TypeConfiguration typeConfiguration,
			FunctionParameterType... parameterTypes) {
		super(
				name,
				new ArgumentTypesValidator(
						StandardArgumentsValidators.between(
							first(functions),
							last(functions)
						),
						parameterTypes
				),
				StandardFunctionReturnTypeResolvers.invariant( type ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, parameterTypes )
		);
		this.functions = functions;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return functions[ arguments.size() ]
				.generateSqmExpression(
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

	public SqmFunctionDescriptor getFunction(int argumentCount) {
		return functions[argumentCount];
	}
}
