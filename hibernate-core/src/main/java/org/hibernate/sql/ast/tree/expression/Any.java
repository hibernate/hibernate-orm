/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * @author Gavin King
 */
public class Any implements Expression, DomainResultProducer {

	private final SelectStatement subquery;
	private final MappingModelExpressible<?> type;

	public Any(SelectStatement subquery, MappingModelExpressible<?> type) {
		this.subquery = subquery;
		this.type = type;
	}

	public SelectStatement getSubquery() {
		return subquery;
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitAny( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JdbcMapping jdbcMapping = type.getSingleJdbcMapping();
		return new BasicResult<>(
				creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
						this,
						jdbcMapping.getJdbcJavaType(),
						null,
						creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
				).getValuesArrayPosition(),
				resultVariable,
				jdbcMapping
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getSingleJdbcMapping().getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}
}
