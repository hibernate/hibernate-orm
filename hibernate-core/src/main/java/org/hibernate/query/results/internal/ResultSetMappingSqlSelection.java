/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * SqlSelection implementation used while building
 * {@linkplain org.hibernate.query.results.ResultSetMapping} references.
 * Doubles as its own {@link Expression} as well.
 *
 * @author Steve Ebersole
 */
public class ResultSetMappingSqlSelection implements SqlSelection, Expression, SqlExpressionAccess {
	private final int valuesArrayPosition;
	private final JdbcMapping valueMapping;
	private final ValueExtractor<?> valueExtractor;

	public ResultSetMappingSqlSelection(int valuesArrayPosition, BasicValuedMapping valueMapping) {
		this ( valuesArrayPosition, valueMapping.getJdbcMapping() );
	}

	public ResultSetMappingSqlSelection(int valuesArrayPosition, JdbcMapping jdbcMapping) {
		this.valuesArrayPosition = valuesArrayPosition;
		this.valueMapping = jdbcMapping;
		this.valueExtractor = jdbcMapping.getJdbcValueExtractor();
	}

	@Override
	public ValueExtractor<?> getJdbcValueExtractor() {
		return valueExtractor;
	}

	@Override
	public SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		return this;
	}

	@Override
	public int getValuesArrayPosition() {
		return valuesArrayPosition;
	}

	@Override
	public Expression getExpression() {
		return this;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return valueMapping;
	}

	@Override
	public boolean isVirtual() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlAstWalker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Expression getSqlExpression() {
		return this;
	}
}
