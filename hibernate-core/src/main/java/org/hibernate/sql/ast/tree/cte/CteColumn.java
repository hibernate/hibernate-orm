/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * Information about a column in the CTE table
 *
 * @author Steve Ebersole
 */
public class CteColumn {
	private final String columnExpression;
	private final JdbcMapping jdbcMapping;

	public CteColumn(String columnExpression, JdbcMapping jdbcMapping) {
		this.columnExpression = columnExpression;
		this.jdbcMapping = jdbcMapping;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
