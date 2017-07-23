/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.sql.ast.tree.spi.select.ResolvedResultSetMapping;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.spi.ResultSetMapping;

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
			ResultSetAccess jdbcResultsAccess,
			SharedSessionContractImplementor persistenceContext) {
		for ( SqlSelection sqlSelection : resolvedMapping.getSqlSelections() ) {
			sqlSelection.prepare( jdbcResultsAccess, persistenceContext );
		}

		return resolvedMapping;
	}
}
