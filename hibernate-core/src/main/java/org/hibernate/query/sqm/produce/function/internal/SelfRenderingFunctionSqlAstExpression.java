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
import org.hibernate.sql.exec.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingFunctionSqlAstExpression implements SelfRenderingExpression, Selectable, SqlSelectable {
	private final SelfRenderingSqmFunction sqmExpression;
	private final List<Expression> sqlAstArguments;

	public SelfRenderingFunctionSqlAstExpression(
			SelfRenderingSqmFunction sqmExpression,
			SqmToSqlAstConverter walker) {
		this.sqmExpression = sqmExpression;
		this.sqlAstArguments = resolveSqlAstArguments( sqmExpression.getSqmArguments(), walker );
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
	public ExpressableType getType() {
		return sqmExpression.getExpressionType();
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(
			Expression selectedExpression,
			String resultVariable) {
		return new BasicValuedNonNavigableSelection( selectedExpression, resultVariable, this );
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicValuedExpressableType) getType() );
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqmExpression.getRenderingSupport().render( sqlAppender, sqlAstArguments,walker, sessionFactory );
	}
}
