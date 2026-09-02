/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.from;

import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

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
