/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.spi.FunctionAsExpressionTemplate;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.GenericParameter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Casts query parameters using the Derby varchar() function
 * before concatenating them using the || operator.
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DerbyConcatEmulation extends FunctionAsExpressionTemplate {

	public DerbyConcatEmulation() {
		super(
				"(", "||", ")",
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING ),
				StandardArgumentsValidators.min( 2 ),
				"concat"
		);
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			SqlAstNode sqlAstArgument,
			SqlAstWalker walker) {
		boolean param = GenericParameter.class.isInstance( sqlAstArgument );
		if ( param ) {
			sqlAppender.appendSql( "cast(" );
		}
		sqlAstArgument.accept(walker);
		if ( param ) {
			sqlAppender.appendSql( " as long varchar)" );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(string0[, string1[, ...]])";
	}
}
