/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
