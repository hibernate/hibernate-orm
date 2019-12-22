/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmFunctionAsExpressionDescriptor extends AbstractSqmFunctionDescriptor {

	private static final Logger log = Logger.getLogger( SqmFunctionAsExpressionDescriptor.class );

	private final String expressionStart;
	private final String argumentSeparator;
	private final String expressionEnd;

	public SqmFunctionAsExpressionDescriptor(
			String expressionStart,
			String argumentSeparator,
			String expressionEnd,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( argumentsValidator, returnTypeResolver );
		this.expressionStart = expressionStart;
		this.argumentSeparator = argumentSeparator;
		this.expressionEnd = expressionEnd;
	}

	@Override
	public FunctionRenderingSupport getRenderingSupport() {
		return (sqlAppender, functionName, sqlAstArguments, walker, sessionFactory) -> {
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
		};
	}
}
