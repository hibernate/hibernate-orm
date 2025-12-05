/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Nathan Xu
 */
class FilterJdbcParameter implements JdbcParameter, JdbcParameterBinder {
	private final JdbcMapping jdbcMapping;
	private final Object jdbcParameterValue;

	FilterJdbcParameter(JdbcMapping jdbcMapping, Object jdbcParameterValue) {
		this.jdbcMapping = jdbcMapping;
		this.jdbcParameterValue = jdbcParameterValue;
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public void bindParameterValue(PreparedStatement statement, int startPosition, JdbcParameterBindings jdbcParameterBindings, ExecutionContext executionContext) throws SQLException {
		jdbcMapping.getJdbcValueBinder().bind(
				statement,
				jdbcMapping.convertToRelationalValue( jdbcParameterValue ),
				startPosition,
				executionContext.getSession()
		);

	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return jdbcMapping;
	}

	@Override
	public @Nullable Integer getParameterId() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new IllegalStateException(  );
	}
}
