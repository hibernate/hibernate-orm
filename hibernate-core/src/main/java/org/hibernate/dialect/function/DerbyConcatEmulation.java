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

import java.util.List;

/**
 * A specialized concat() function definition in which:<ol>
 * <li>we translate to use the concat operator ('||')</li>
 * <li>wrap dynamic parameters in CASTs to VARCHAR</li>
 * </ol>
 * <p/>
 * This last spec is to deal with a limitation on DB2 and variants (e.g. Derby)
 * where dynamic parameters cannot be used in concatenation unless they are being
 * concatenated with at least one non-dynamic operand.  And even then, the rules
 * are so convoluted as to what is allowed and when the CAST is needed and when
 * it is not that we just go ahead and do the CASTing.
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
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker) {

		// check if all arguments are parameters...
		boolean areAllArgumentsDynamic = true;
		for ( SqlAstNode argument : sqlAstArguments ) {
			if ( GenericParameter.class.isInstance( argument ) ) {
				areAllArgumentsDynamic = false;
				break;
			}
		}

		if ( areAllArgumentsDynamic ) {
			// - if so, wrap the individual args in `varchar()`
			//   and wrap the entire expression in a `cast()`
			sqlAppender.appendSql( "cast(" );
			super.render( sqlAppender, sqlAstArguments, walker );
			sqlAppender.appendSql( " as varchar(32672))" );
		}
		else {
			// - if not, simply use the Derby concat operator
			//   `arg1 || arg2`
			super.render( sqlAppender, sqlAstArguments, walker );
		}
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			SqlAstNode sqlAstArgument,
			SqlAstWalker walker) {
		sqlAppender.appendSql( "varchar(" );
		sqlAstArgument.accept(walker);
		sqlAppender.appendSql( ")" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(string0[, string1[, ...]])";
	}
}
