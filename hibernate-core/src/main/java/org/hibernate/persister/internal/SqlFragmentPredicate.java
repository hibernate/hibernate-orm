/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.internal;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Predicate based on a SQL fragment
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
