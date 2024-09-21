/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
