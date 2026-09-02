/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * Predicate based on a SQL fragment
 *
 * @author Steve Ebersole
 */
public class SqlFragmentPredicate implements Predicate {
	private final String fragment;

	public SqlFragmentPredicate(String fragment) {
		this.fragment = fragment;
	}

	public String getSqlFragment() {
		return fragment;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSqlFragmentPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
