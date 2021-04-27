/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Models a predicate in the SQL AST
 *
 * @author Steve Ebersole
 */
public interface Predicate extends SqlAstNode {
	/**
	 * Short-cut for {@link SqlAstTreeHelper#combinePredicates}
	 */
	static Predicate combinePredicates(Predicate p1, Predicate p2) {
		return SqlAstTreeHelper.combinePredicates( p1, p2 );
	}

	boolean isEmpty();
}
