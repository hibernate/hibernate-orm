/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.sql.exec.results.spi.QueryResult;
import org.hibernate.sql.exec.results.spi.ResolvedResultSetMapping;
import org.hibernate.sql.exec.results.spi.SqlSelection;
import org.hibernate.sql.exec.results.spi.ResultSetMapping;

/**
 * ResultSetMapping for handling selections for a {@link org.hibernate.query.NativeQuery}
 * which (partially) defines its result mappings.  At the very least we will need
 * to resolve the `columnAlias` to its ResultSet index.  For scalar results
 * ({@link javax.persistence.ColumnResult}) we may additionally need to resolve
 * its "type" for reading.
 *
 * Specifically needs to
 * @author Steve Ebersole
 */
public class ResultSetMappingDefinedImpl implements ResultSetMapping {
	private final ResolvedResultSetMapping resolvedMapping;

	public ResultSetMappingDefinedImpl(List<SqlSelection> selections, List<QueryResult> queryResults) {
		resolvedMapping  = new ResolvedResultSetMapping() {
			@Override
			public List<SqlSelection> getSqlSelections() {
				return selections;
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
		for ( SqlSelection sqlSelection : resolvedMapping.getSqlSelections() ) {
			sqlSelection.prepare( jdbcResultsMetadata, resolutionContext );
		}

		return resolvedMapping;
	}
}
