/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import jakarta.annotation.Nullable;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.WindowFunctionExpression;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Representation of a window function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Christian Beikov
 */
public class SelfRenderingWindowFunctionSqlAstExpression<T> extends SelfRenderingFunctionSqlAstExpression<T>
		implements WindowFunctionExpression {

	private final Predicate filter;
	private final @Nullable Boolean respectNulls;
	private final @Nullable Boolean fromFirst;

	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public SelfRenderingWindowFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			@Nullable Boolean respectNulls,
			@Nullable Boolean fromFirst,
			@Nullable ReturnableType<T> type,
			@Nullable JdbcMappingContainer expressible) {
		super( functionName, renderer, sqlAstArguments, type, expressible );
		this.filter = filter;
		this.respectNulls = respectNulls;
		this.fromFirst = fromFirst;
	}

	@Override
	public Predicate getFilter() {
		return filter;
	}

	@Override
	@Nullable
	public Boolean getRespectNulls() {
		return respectNulls;
	}

	@Override
	@Nullable
	public Boolean getFromFirst() {
		return fromFirst;
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		getFunctionRenderer().render( sqlAppender, getArguments(), filter, respectNulls, fromFirst, getType(), walker );
	}
}
