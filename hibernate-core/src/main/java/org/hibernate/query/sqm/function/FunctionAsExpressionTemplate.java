/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FunctionAsExpressionTemplate
		extends AbstractSqmFunctionDescriptor
		implements FunctionRenderingSupport {

	private static final Logger log = Logger.getLogger( FunctionAsExpressionTemplate.class );

	private final String expressionStart;
	private final String argumentSeparator;
	private final String expressionEnd;

	public FunctionAsExpressionTemplate(
			String expressionStart,
			String argumentSeparator,
			String expressionEnd,
			ArgumentsValidator argumentsValidator) {
		super( argumentsValidator );
		this.expressionStart = expressionStart;
		this.argumentSeparator = argumentSeparator;
		this.expressionEnd = expressionEnd;
	}

	@Override
	protected FunctionRenderingSupport getRenderingSupport() {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			String functionName, List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( expressionStart );

		if ( sqlAstArguments.isEmpty() ) {
			log.debugf( "No arguments found for FunctionAsExpressionTemplate, this is most likely a query syntax error" );
		}
		else {
			// render the first argument..
			sqlAstArguments.get( 0 ).accept( walker );

			// render the rest of the arguments, preceded by the separator
			for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
				sqlAppender.appendSql( argumentSeparator );
				sqlAstArguments.get( i ).accept( walker );
			}
		}

		sqlAppender.appendSql( expressionEnd );
	}
}
