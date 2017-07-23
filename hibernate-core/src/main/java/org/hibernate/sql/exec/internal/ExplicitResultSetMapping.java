/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.sql.ast.tree.spi.select.ResolvedResultSetMapping;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.spi.ResultSetMapping;

/**
 * @author Steve Ebersole
 */
public class ExplicitResultSetMapping implements ResultSetMapping {
	private final ResolvedResultSetMapping resolvedMapping;

	public ExplicitResultSetMapping(List<SqlSelection> sqlSelections, List<QueryResult> queryResults) {
		resolvedMapping = new ResolvedResultSetMapping() {
			@Override
			public List<SqlSelection> getSqlSelections() {
				return sqlSelections;
			}

			@Override
			public List<QueryResult> getQueryResults() {
				return queryResults;
			}
		};
	}

	@Override
	public ResolvedResultSetMapping resolve(
			ResultSetAccess jdbcResultsAccess,
			SharedSessionContractImplementor persistenceContext) {
		return resolvedMapping;
	}
}
