/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.sql.ast.produce.spi.SqmFunction;
import org.hibernate.sql.ast.produce.sqm.spi.SqmExpressionInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends AbstractSqmExpression<T> implements SqmFunction<T> {
	private final String name;
	private final SelfRenderingFunctionSupport renderingSupport;
	private final List<SqmTypedNode<?>> arguments;

	public SelfRenderingSqmFunction(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			NodeBuilder nodeBuilder,
			String name) {
		super( impliedResultType, nodeBuilder );
		this.renderingSupport = renderingSupport;
		this.arguments = arguments;
		this.name = name;
	}

	public List<SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	@Override
	public AllowableFunctionReturnType<T> getExpressableType() {
		return (AllowableFunctionReturnType<T>) super.getExpressableType();
	}

	public SelfRenderingFunctionSupport getRenderingSupport() {
		return renderingSupport;
	}

	private static SqlAstNode toSqlAstNode(Object arg, SqmToSqlAstConverter walker) {
		if (arg instanceof SqmExpressionInterpretation) {
			return ( (SqmExpressionInterpretation) arg ).toSqlExpression( walker );
		}
		return (SqlAstNode) arg;
	}

	private static List<SqlAstNode> resolveSqlAstArguments(List<SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
			return emptyList();
		}

		final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>();
		for ( SqmTypedNode sqmArgument : sqmArguments ) {
			sqlAstArguments.add( toSqlAstNode( ((SqmVisitableNode) sqmArgument).accept( walker ), walker ) );
		}
		return sqlAstArguments;
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
		AllowableFunctionReturnType<T> type = getExpressableType();
		return new SelfRenderingFunctionSqlAstExpression(
				getRenderingSupport(),
				resolveSqlAstArguments( getArguments(), walker),
				type ==null ? null : type.getSqlExpressableType()
		);
	}

	@Override
	public String getFunctionName() {
		return name;
	}

}
