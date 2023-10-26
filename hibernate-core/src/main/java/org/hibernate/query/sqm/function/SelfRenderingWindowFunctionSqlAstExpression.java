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
import org.hibernate.sql.ast.tree.expression.WindowFunctionExpression;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Representation of a window function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Christian Beikov
 */
public class SelfRenderingWindowFunctionSqlAstExpression extends SelfRenderingFunctionSqlAstExpression
		implements WindowFunctionExpression {

	private final Predicate filter;
	private final Boolean respectNulls;
	private final Boolean fromFirst;

	/**
	 * @deprecated Use {@link #SelfRenderingWindowFunctionSqlAstExpression(String, FunctionRenderer, List, Predicate, Boolean, Boolean, ReturnableType, JdbcMappingContainer)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingWindowFunctionSqlAstExpression(
			String functionName,
			FunctionRenderingSupport renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> type,
			JdbcMappingContainer expressible) {
		super( functionName, renderer, sqlAstArguments, type, expressible );
		this.filter = filter;
		this.respectNulls = respectNulls;
		this.fromFirst = fromFirst;
	}

	public SelfRenderingWindowFunctionSqlAstExpression(
			String functionName,
			FunctionRenderer renderer,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> type,
			JdbcMappingContainer expressible) {
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
	public Boolean getRespectNulls() {
		return respectNulls;
	}

	@Override
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
