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
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * @author Steve Ebersole
 */
public class SqlAstSelectDescriptorImpl extends AbstractSqlAstDescriptor implements SqlAstSelectDescriptor {
	private final List<DomainResult> queryReturns;

	public SqlAstSelectDescriptorImpl(
			SelectStatement selectQuery,
			List<DomainResult> queryReturns,
			Set<String> affectedTables) {
		super( selectQuery, affectedTables );
		this.queryReturns = queryReturns;
	}

	@Override
	public SelectStatement getSqlAstStatement() {
		return (SelectStatement) super.getSqlAstStatement();
	}

	@Override
	public List<DomainResult> getQueryResults() {
		return queryReturns;
	}
}
