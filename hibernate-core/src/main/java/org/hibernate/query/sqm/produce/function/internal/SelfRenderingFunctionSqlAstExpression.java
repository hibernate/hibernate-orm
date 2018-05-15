/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
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
	private final SelfRenderingSqmFunction sqmExpression;
	private final List<Expression> sqlAstArguments;
	private final TypeConfiguration typeConfiguration;

	public SelfRenderingFunctionSqlAstExpression(
			SelfRenderingSqmFunction sqmExpression,
			SqmToSqlAstConverter walker) {
		this.sqmExpression = sqmExpression;
		this.sqlAstArguments = resolveSqlAstArguments( sqmExpression.getSqmArguments(), walker );
		this.typeConfiguration = walker.getSessionFactory().getTypeConfiguration();
	}

	private static List<Expression> resolveSqlAstArguments(List<SqmExpression> sqmArguments, SqmToSqlAstConverter walker) {
		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
			return Collections.emptyList();
		}

		final ArrayList<Expression> sqlAstArguments = new ArrayList<>();
		for ( SqmExpression sqmArgument : sqmArguments ) {
			sqlAstArguments.add( (Expression) sqmArgument.accept( walker ) );
		}
		return sqlAstArguments;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return ( (BasicValuedExpressableType) getType() ).getBasicType().getSqlExpressableType( typeConfiguration );
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
				null,
				getExpressableType()
		);
	}


	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getExpressableType().getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getExpressableType()
		);
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqmExpression.getRenderingSupport().render( sqlAppender, sqlAstArguments,walker, sessionFactory );
	}
}
