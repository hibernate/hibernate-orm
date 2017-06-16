/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal.instantiation;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.spi.RowProcessingState;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class QueryResultAssemblerDynamicInstantiation implements QueryResultAssembler {
	private final Class target;

	public QueryResultAssemblerDynamicInstantiation(Class target, List<SqlSelection> sqlSelections) {

		this.target = target;
	}

	@Override
	public Class getReturnedJavaType() {
		return target;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		return null;
	}
}
