/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * A column physically defined in the database table or inline-view
 *
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final Identifier columnName;
	private final JdbcMapping jdbcMapping;

	public PhysicalColumn(Table table, Identifier columnName, JdbcMapping jdbcMapping) {
		this.table = table;
		this.columnName = columnName;
		this.jdbcMapping = jdbcMapping;
	}

	public Identifier getColumnName() {
		return columnName;
	}

	@Override
	public String getColumnExpression() {
		return getColumnName().render();
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
