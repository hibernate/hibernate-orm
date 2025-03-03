/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * Models a predicate in the SQL AST
 *
 * @author Steve Ebersole
 */
public interface Predicate extends Expression, DomainResultProducer<Boolean> {
	/**
	 * Short-cut for {@link SqlAstTreeHelper#combinePredicates}
	 */
	static Predicate combinePredicates(Predicate p1, Predicate p2) {
		return SqlAstTreeHelper.combinePredicates( p1, p2 );
	}

	boolean isEmpty();

	@Override
	default DomainResult<Boolean> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final JdbcMapping jdbcMapping = getExpressionType().getSingleJdbcMapping();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				this,
				jdbcMapping.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);

		return new BasicResult<>( sqlSelection.getValuesArrayPosition(), resultVariable, jdbcMapping );
	}

	@Override
	default void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				getExpressionType().getSingleJdbcMapping().getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}
}
