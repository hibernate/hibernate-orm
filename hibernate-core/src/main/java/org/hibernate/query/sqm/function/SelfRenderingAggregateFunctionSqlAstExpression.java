/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Representation of an aggregate function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Christian Beikov
 */
public class SelfRenderingAggregateFunctionSqlAstExpression<T> extends SelfRenderingFunctionSqlAstExpression<T>
		implements AggregateFunctionExpression {

	private final Predicate filter;

	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public SelfRenderingAggregateFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<T> type,
			JdbcMappingContainer expressible) {
		super( functionName, renderer, sqlAstArguments, type, expressible );
		this.filter = filter;
	}

	@Override
	public Predicate getFilter() {
		return filter;
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		getFunctionRenderer().render( sqlAppender, getArguments(), filter, getType(), walker );
	}
}
