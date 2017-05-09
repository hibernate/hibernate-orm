/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.select.Selectable;

/**
 * @author Steve Ebersole
 */
public interface Expression {
	/**
	 * Access the type for this expression.
	 */
	ExpressableType getType();

	Selectable getSelectable();

	void accept(SqlSelectAstToJdbcSelectConverter walker);
}
