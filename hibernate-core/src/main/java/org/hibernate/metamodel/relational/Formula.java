/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * A read-only expression (e.g. `x + 1`) to which a ModelPart is mapped
 *
 * @author Steve Ebersole
 */
public class Formula implements Column {
	private final Table table;
	private final String expression;
	private final JdbcMapping jdbcMapping;

	public Formula(Table table, String expression, JdbcMapping jdbcMapping) {
		this.table = table;
		this.expression = expression;
		this.jdbcMapping = jdbcMapping;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public String getColumnExpression() {
		return getExpression();
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
