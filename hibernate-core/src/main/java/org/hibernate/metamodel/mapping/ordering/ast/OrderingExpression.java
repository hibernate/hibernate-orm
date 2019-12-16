/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.SortOrder;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * Contract for anything that can be a sort expression
 *
 * @author Steve Ebersole
 */
public interface OrderingExpression extends Node {
	/**
	 * Apply the SQL AST sort-specifications associated with this ordering-expression
	 */
	void apply(QuerySpec ast, TableGroup tableGroup, String collation, SortOrder sortOrder, SqlAstCreationState creationState);
}
