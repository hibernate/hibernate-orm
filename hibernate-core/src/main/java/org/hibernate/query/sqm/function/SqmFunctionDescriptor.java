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
 * Extension for supplying support for non-standard (ANSI SQL) functions in HQL and Criteria queries.
 *
 * Ultimately acts as a factory for SQM function expressions.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
@Incubating
public interface SqmFunctionDescriptor {
	/**
	 * Generate a representation of the described function as a SQL AST node
	 */
	Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState);

}
