/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * The standard Hibernate implementation of SqlAstSelectDescriptor.
 *
 * @author Steve Ebersole
 */
public class SqlAstSelectDescriptorImpl implements SqlAstSelectDescriptor {
	private final SelectStatement selectQuery;
	private final List<DomainResult> queryReturns;
	private final Set<String> affectedTables;

	public SqlAstSelectDescriptorImpl(
			SelectStatement selectQuery,
			List<DomainResult> queryReturns,
			Set<String> affectedTables) {
		this.selectQuery = selectQuery;
		this.queryReturns = queryReturns;
		this.affectedTables = affectedTables;
	}

	@Override
	public SelectStatement getSqlAstStatement() {
		return selectQuery;
	}

	@Override
	public List<DomainResult> getQueryResults() {
		return queryReturns;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTables;
	}
}
