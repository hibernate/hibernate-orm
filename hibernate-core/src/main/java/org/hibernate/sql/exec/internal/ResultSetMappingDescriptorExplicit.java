/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * ResultSetMappingDescriptor implementation for cases where the mappings are
 * all explicitly created by Hibernate itself.  Basically this covers all
 * scenarios *except* {@link org.hibernate.query.NativeQuery} processing -
 * an important distinction as it means we do not have to perform any
 * {@link java.sql.ResultSetMetaData} resolutions.
 *
 * @author Steve Ebersole
 */
public class ResultSetMappingDescriptorExplicit implements ResultSetMappingDescriptor {
	private final ResultSetMapping resolvedMapping;

	public ResultSetMappingDescriptorExplicit(List<SqlSelection> sqlSelections, List<QueryResult> queryResults) {
		resolvedMapping = new ResultSetMapping() {
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
	public ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			ResolutionContext resolutionContext) {
		return resolvedMapping;
	}
}
