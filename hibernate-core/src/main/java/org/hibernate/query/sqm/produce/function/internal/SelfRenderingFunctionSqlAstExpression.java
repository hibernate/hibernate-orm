/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.Selectable;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Representation of a function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Steve Ebersole
 */
public class SelfRenderingFunctionSqlAstExpression
		implements SelfRenderingExpression, Selectable, SqlExpressable, DomainResultProducer {
	private final SelfRenderingFunctionSupport renderer;
	private final List<SqlAstNode> sqlAstArguments;
	private final SqlExpressableType type;

	public SelfRenderingFunctionSqlAstExpression(
			SelfRenderingFunctionSupport renderer,
			List<SqlAstNode> sqlAstArguments,
			SqlExpressableType type) {
		this.renderer = renderer;
		this.sqlAstArguments = sqlAstArguments;
		this.type = type;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}


	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		SqlExpressableType type = getExpressableType();
		if ( type==null ) {
			throw new SemanticException("function return type is unknown, so function cannot occur in select");
		}
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						type.getJavaTypeDescriptor(),
						creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
				),
				type
		);
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		renderer.render( sqlAppender, sqlAstArguments, walker);
	}
}
