/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Functional contract for producing the join-predicate related to a {@link TableReferenceJoin}.
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public
interface TableReferenceJoinPredicateProducer {
	Predicate producePredicate(TableReference lhs, TableReference rhs, SqlAstJoinType sqlAstJoinType);
}
