/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.sql.exec.results.spi.QueryResult;
import org.hibernate.sql.exec.results.spi.ResolvedResultSetMapping;
import org.hibernate.sql.exec.results.spi.SqlSelection;
import org.hibernate.sql.exec.results.spi.ResultSetMapping;

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
			JdbcValuesMetadata jdbcResultsMetadata,
			ResolutionContext resolutionContext) {
		return resolvedMapping;
	}
}
