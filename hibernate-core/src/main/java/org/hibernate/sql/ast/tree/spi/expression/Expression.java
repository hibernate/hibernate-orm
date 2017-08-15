/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.predicate.SqlAstNode;
import org.hibernate.sql.results.spi.Selectable;

/**
 * Models an expression at the SQL-level.
 *
 * Note that these Expressions can also model a reference to a domain-model Navigable.
 * While not technically a SQL-level expression, modeling them as such makes handling
 * the easier later while processing the SQL AST to generate the appropriate
 * {@link org.hibernate.sql.exec.spi.JdbcOperation}
 *
 *
 *
 * @author Steve Ebersole
 */
public interface Expression extends SqlAstNode, Selectable {
	/**
	 * Access the type for this expression.  See {@link ExpressableType}
	 * for more detailed description.
	 */
	ExpressableType getType();
}
