/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.OrderedSetAggregateFunctionExpression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Representation of an aggregate function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Christian Beikov
 */
public class SelfRenderingOrderedSetAggregateFunctionSqlAstExpression extends SelfRenderingAggregateFunctionSqlAstExpression
		implements OrderedSetAggregateFunctionExpression {

	private final List<SortSpecification> withinGroup;

	/**
	 * @deprecated Use {@link #SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(String, FunctionRenderer, List, Predicate, List, ReturnableType, JdbcMappingContainer)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(
			String functionName,
			FunctionRenderingSupport renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> type,
			JdbcMappingContainer expressible) {
		super( functionName, renderer, sqlAstArguments, filter, type, expressible );
		this.withinGroup = withinGroup;
	}

	public SelfRenderingOrderedSetAggregateFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> type,
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
