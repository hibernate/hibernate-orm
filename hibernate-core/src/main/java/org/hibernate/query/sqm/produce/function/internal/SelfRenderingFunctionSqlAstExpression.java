/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.produce.sqm.spi.SqmExpressionInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.Selectable;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.emptyList;

/**
 * Representation of a function call in the SQL AST for impls that know how to
 * render themselves.
 *
 * @author Steve Ebersole
 */
public class SelfRenderingFunctionSqlAstExpression<T>
		implements SelfRenderingExpression, Selectable, SqlExpressable, DomainResultProducer {
	private final SelfRenderingSqmFunction sqmExpression;
	private final List<SqlAstNode> sqlAstArguments;
	private final SqlExpressableType type;

	SelfRenderingFunctionSqlAstExpression(
			SelfRenderingSqmFunction<T> sqmFunction,
			SqmToSqlAstConverter walker) {
		this.sqmExpression = sqmFunction;
		this.sqlAstArguments = resolveSqlAstArguments( sqmFunction.getArguments(), walker );
		this.type = sqmFunction.getNodeType().getSqlExpressableType();
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

	private static SqlAstNode toSqlAstNode(Object arg, SqmToSqlAstConverter walker) {
		if (arg instanceof SqmExpressionInterpretation) {
			return ( (SqmExpressionInterpretation) arg ).toSqlExpression( walker );
		}
		return (SqlAstNode) arg;
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
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getExpressableType().getJavaTypeDescriptor(),
						creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
				),
				getExpressableType()
		);
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqmExpression.getRenderingSupport().render( sqlAppender, sqlAstArguments, walker, sessionFactory );
	}
}
