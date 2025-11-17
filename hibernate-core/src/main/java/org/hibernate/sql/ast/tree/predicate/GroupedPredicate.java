/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class GroupedPredicate implements Predicate {
	private final Predicate subPredicate;

	public GroupedPredicate(Predicate subPredicate) {
		this.subPredicate = subPredicate;
	}

	public Predicate getSubPredicate() {
		return subPredicate;
	}

	@Override
	public boolean isEmpty() {
		return subPredicate.isEmpty();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitGroupedPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return subPredicate.getExpressionType();
	}
}
