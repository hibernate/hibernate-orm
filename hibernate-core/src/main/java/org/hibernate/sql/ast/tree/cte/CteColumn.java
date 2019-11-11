/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.tree.cte.CteTable;

/**
 * @author Steve Ebersole
 */
public class CteColumn {
	private final CteTable cteTable;
	private final String columnExpression;
	private final JdbcMapping jdbcMapping;

	public CteColumn(CteTable cteTable, String columnExpression, JdbcMapping jdbcMapping) {
		this.cteTable = cteTable;
		this.columnExpression = columnExpression;
		this.jdbcMapping = jdbcMapping;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
