/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Support for overloaded functions defined in terms of a
 * list of patterns, one for each possible function arity.
 *
 * @see PatternBasedSqmFunctionDescriptor
 *
 * @author Gavin King
 */
public class MultiPatternSqmFunctionDescriptor implements SqmFunctionDescriptor {
	private final SqmFunctionDescriptor[] functions;
	private final ArgumentsValidator argumentsValidator;


	private static int first(SqmFunctionDescriptor[] functions) {
		for ( int i = 0; i < functions.length; i++ ) {
			if ( functions[i] != null ) {
				return i;
			}
		}
		throw new IllegalArgumentException( "no functions" );
	}

	private static int last(SqmFunctionDescriptor[] functions) {
		return functions.length;
	}

	/**
	 * Construct an instance with the given function templates
	 * where the position of each function template in the
	 * given array corresponds to the arity of the function
	 * template. The array must be padded with leading nulls
	 * where there is no overloaded form corresponding to
	 * lower arities.
	 *
	 * @param functionDescriptors the function templates to delegate to,
	 *                  where array position corresponds to
	 *                  arity.
	 */
	public MultiPatternSqmFunctionDescriptor(SqmFunctionDescriptor[] functionDescriptors) {
		argumentsValidator = StandardArgumentsValidators.between(
				first( functionDescriptors ),
				last( functionDescriptors )
		);
		this.functions = functionDescriptors;
	}

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		argumentsValidator.validate( arguments );
		return functions[ arguments.size() ].generateSqlExpression(
				functionName, arguments, inferableTypeAccess, converter, creationState
		);
	}
}
