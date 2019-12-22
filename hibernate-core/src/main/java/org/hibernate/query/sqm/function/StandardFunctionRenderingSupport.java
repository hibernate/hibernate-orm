/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * FunctionRenderingSupport
 * @author Steve Ebersole
 */
public class StandardFunctionRenderingSupport implements FunctionRenderingSupport {
	public static final StandardFunctionRenderingSupport PAREN = new StandardFunctionRenderingSupport( true );
	public static final StandardFunctionRenderingSupport NO_PAREN = new StandardFunctionRenderingSupport( false );

	private final boolean useParenthesesWhenNoArgs;

	public StandardFunctionRenderingSupport(boolean useParenthesesWhenNoArgs) {
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			String functionName,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		final boolean useParens = useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty();

		sqlAppender.appendSql( functionName );
		if ( useParens ) {
			sqlAppender.appendSql( "(" );
		}

		boolean firstPass = true;
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			if ( !firstPass ) {
				sqlAppender.appendSql( ", " );
			}
			sqlAstArgument.accept( walker );
			firstPass = false;
		}

		if ( useParens ) {
			sqlAppender.appendSql( ")" );
		}
	}
}
