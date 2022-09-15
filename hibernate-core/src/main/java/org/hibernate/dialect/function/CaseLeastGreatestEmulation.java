/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.COMPARABLE;

/**
 * Some databases don't have a function like {@code least()} or {@code greatest()},
 * and on those platforms we emulate the function using {@code case}.
 *
 * @author Christian Beikov
 */
public class CaseLeastGreatestEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String operator;

	public CaseLeastGreatestEmulation(boolean least) {
		super(
				least ? StandardFunctions.LEAST : StandardFunctions.GREATEST,
				new ArgumentTypesValidator( StandardArgumentsValidators.min( 2 ), COMPARABLE, COMPARABLE ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull(),
				StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE
		);
		this.operator = least ? "<=" : ">=";
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		if ( numberOfArguments > 1 ) {
			final int lastArgument = numberOfArguments - 1;
			sqlAppender.appendSql( "case" );
			for ( int i = 0; i < lastArgument; i++ ) {
				sqlAppender.appendSql( " when " );
				String separator = "";
				for ( int j = i + 1; j < numberOfArguments; j++ ) {
					sqlAppender.appendSql( separator );
					arguments.get( i ).accept( walker );
					sqlAppender.appendSql( operator );
					arguments.get( j ).accept( walker );
					separator = " and ";
				}
				sqlAppender.appendSql( " then " );
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
		return "(COMPARABLE arg0[, COMPARABLE arg1[, ...]])";
	}
}
