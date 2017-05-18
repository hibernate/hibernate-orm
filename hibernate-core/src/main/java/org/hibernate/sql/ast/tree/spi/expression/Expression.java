/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.queryable.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.predicate.SqlAstNode;
import org.hibernate.sql.ast.tree.spi.select.Selectable;

/**
 * @author Steve Ebersole
 */
public interface Expression extends SqlAstNode {
	/**
	 * Access the type for this expression.  See {@link ExpressableType}
	 * for more detailed description.
	 */
	ExpressableType getType();

	/**
	 * For expressions that may be part of the SELECT clause, return the
	 * "selectable" representation of that expression.  Again, see
	 * {@link Selectable} for mode detailed description
	 */
	Selectable getSelectable();
}
