/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;

/**
 *
 * @author Christian Beikov
 */
public class ResolvedSqlSelection extends SqlSelectionImpl {

	private final BasicType<Object> resolvedType;

	public ResolvedSqlSelection(
			int valuesArrayPosition,
			Expression sqlExpression,
			BasicType<Object> resolvedType) {
		super( valuesArrayPosition + 1, valuesArrayPosition, sqlExpression, null, false, resolvedType.getJdbcValueExtractor() );
		this.resolvedType = resolvedType;
	}

	public ResolvedSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			Expression sqlExpression,
			BasicType<Object> resolvedType) {
		super( jdbcPosition, valuesArrayPosition, sqlExpression, null, false, resolvedType.getJdbcValueExtractor() );
		this.resolvedType = resolvedType;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return resolvedType;
	}

	@Override
	public boolean isVirtual() {
		return false;
	}

	@Override
	public SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		return this;
	}
}
