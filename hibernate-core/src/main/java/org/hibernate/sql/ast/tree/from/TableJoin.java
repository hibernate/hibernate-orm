/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
