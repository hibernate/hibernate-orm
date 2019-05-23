/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.List;

/**
 * @author Gavin King
 */
public class PairedFunctionTemplate extends AbstractSqmFunctionTemplate {

	private SqmFunctionTemplate binaryFunction;
	private SqmFunctionTemplate ternaryFunction;

	public PairedFunctionTemplate(
			SqmFunctionTemplate binaryFunction,
			SqmFunctionTemplate ternaryFunction) {
		super( StandardArgumentsValidators.between( 2, 3 ) );
		this.binaryFunction = binaryFunction;
		this.ternaryFunction = ternaryFunction;
	}

	public static void register(
			QueryEngine queryEngine,
			String name,
			AllowableFunctionReturnType type,
			String pattern2,
			String pattern3) {
		queryEngine.getSqmFunctionRegistry().register(
				name,
				new PairedFunctionTemplate(
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( name, pattern2 )
								.setExactArgumentCount( 2 )
								.setInvariantType( type)
								.template(),
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( name, pattern3 )
								.setExactArgumentCount( 3 )
								.setInvariantType( type )
								.template()
				)
		);
	}


	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return ( arguments.size()<3 ? binaryFunction : ternaryFunction )
				.makeSqmFunctionExpression( arguments, impliedResultType, queryEngine );
	}
}
