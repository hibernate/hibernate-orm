/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionImpl implements SqlSelection, SqlExpressionAccess {
	private final ColumnReference columnReference;
	private final int valuesArrayPosition;

	public SqlSelectionImpl(ColumnReference columnReference, int valuesArrayPosition) {
		this.columnReference = columnReference;
		this.valuesArrayPosition = valuesArrayPosition;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionAccess

	@Override
	public Expression getSqlExpression() {
		return columnReference;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		return columnReference.getJdbcMapping().getJdbcValueExtractor();
	}

	@Override
	public int getValuesArrayPosition() {
		return valuesArrayPosition;
	}

	@Override
	public Expression getExpression() {
		return columnReference;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return columnReference.getExpressionType();
	}

	@Override
	public boolean isVirtual() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlAstWalker) {
		sqlAstWalker.visitSqlSelection( this );
	}

	@Override
	public SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		return null;
	}
}
