/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 *
 * @author Christian Beikov
 */
public class QuantifiedLeastGreatestEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String operator;

	public QuantifiedLeastGreatestEmulation(boolean least) {
		super(
				least ? "least" : "greatest",
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull()
		);
		this.operator = least ? "<=" : ">=";
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		if ( numberOfArguments > 1 ) {
			final int lastArgument = numberOfArguments - 1;
			sqlAppender.appendSql( "case" );
			for ( int i = 0; i < lastArgument; i++ ) {
				sqlAppender.appendSql( " when " );
				arguments.get( i ).accept( walker );
				sqlAppender.appendSql( operator );
				sqlAppender.appendSql( "all(" );
				String separator = "";
				for ( int j = i + 1; j < numberOfArguments; j++ ) {
					sqlAppender.appendSql( separator );
					arguments.get( j ).accept( walker );
					separator = ",";
				}
				sqlAppender.appendSql( ") then " );
				arguments.get( i ).accept( walker );
			}
			sqlAppender.appendSql( " else " );
			arguments.get( lastArgument ).accept( walker );
			sqlAppender.appendSql( " end" );
		}
		else {
			arguments.get( 0 ).accept( walker );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(arg0[, arg1[, ...]])";
	}
}
