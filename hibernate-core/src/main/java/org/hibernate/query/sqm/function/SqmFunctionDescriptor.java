/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Support for functions in HQL and Criteria queries. Each instance represents
 * a particular function, determining the type and signature of the function,
 * and acting as a factory for SQM function {@link Expression}s.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
@Incubating
public interface SqmFunctionDescriptor {
	/**
	 * Generate a representation of the described function as a SQL AST node.
	 */
	Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState);

	/**
	 * A representatiom of the function signature suitable for display to
	 * the user.
	 *
	 * @param functionName the functionName of the function
	 */
	default String getSignature(String functionName) {
		return functionName;
	}

	/**
	 * Determines if invocations of this function require an argument
	 * list. (Some SQL functions, for example {@code current_date}, do
	 * not require an argument list.
	 *
	 * @return false if the function can be called without parentheses
	 */
	default boolean requiresArgumentList() {
		return true;
	}
}
