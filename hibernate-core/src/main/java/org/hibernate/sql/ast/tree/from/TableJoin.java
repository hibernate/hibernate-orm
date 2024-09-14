/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * The commonalities between {@link TableGroupJoin} and {@link TableReferenceJoin}.
 *
 * @author Christian Beikov
 */
public interface TableJoin extends SqlAstNode {
	SqlAstJoinType getJoinType();
	Predicate getPredicate();
	SqlAstNode getJoinedNode();
	boolean isInitialized();
}
