/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

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
public class ResultSetMappingDescriptorDefined implements ResultSetMappingDescriptor {
	private final ResultSetMapping resolvedMapping;

	public ResultSetMappingDescriptorDefined(List<SqlSelection> selections, List<QueryResult> queryResults) {
		resolvedMapping  = new ResultSetMapping() {
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
	public ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			ResolutionContext resolutionContext) {
		for ( SqlSelection sqlSelection : resolvedMapping.getSqlSelections() ) {
			sqlSelection.prepare( jdbcResultsMetadata, resolutionContext );
		}

		return resolvedMapping;
	}
}
