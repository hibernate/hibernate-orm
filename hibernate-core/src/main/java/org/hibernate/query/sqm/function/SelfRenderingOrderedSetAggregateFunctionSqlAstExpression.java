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
import org.hibernate.sql.ast.spi.query.expression.OrderedSetAggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;

/**
 * Representation of an aggregate function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Christian Beikov
 */
public class SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<T>
		extends SelfRenderingAggregateFunctionSqlAstExpression<T>
		implements OrderedSetAggregateFunctionExpression {

	private final List<SortSpecification> withinGroup;

	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<T> type,
			JdbcMappingContainer expressible) {
		super( functionName, renderer, sqlAstArguments, filter, type, expressible );
		this.withinGroup = withinGroup;
	}

	@Override
	public List<SortSpecification> getWithinGroup() {
		return withinGroup;
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		getFunctionRenderer().render( sqlAppender, getArguments(), getFilter(), withinGroup, getType(), walker );
	}
}
