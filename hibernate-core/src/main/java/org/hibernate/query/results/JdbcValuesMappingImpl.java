/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.List;

import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.internal.StandardJdbcValuesMapping;

/**
 * Implementation of JdbcValuesMapping for native / procedure queries
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingImpl extends StandardJdbcValuesMapping {

	private final int rowSize;

	public JdbcValuesMappingImpl(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults, int rowSize) {
		super( sqlSelections, domainResults );
		this.rowSize = rowSize;
	}

	@Override
	public int getRowSize() {
		return rowSize;
	}
}
