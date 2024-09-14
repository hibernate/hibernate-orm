/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Models a function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface FunctionExpression extends Expression {

	String getFunctionName();

	List<? extends SqlAstNode> getArguments();
}
