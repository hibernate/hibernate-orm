/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;

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
public class DerbyConcatFunctionTemplate extends ConcatFunctionTemplate {

	public static final DerbyConcatFunctionTemplate INSTANCE = new DerbyConcatFunctionTemplate();

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		// check if all arguments are parameters...
		//		- if not, simply use the Derby concat operator - e.g. `arg1 || arg2`
		//		- if so, wrap the individual args in `cast` function and wrap the
		//				entire expression in Derby's `varchar` function (specialized
		// 				`cast` function)
		boolean areAllArgumentsDynamic = true;
		for ( Expression argument : sqlAstArguments ) {
			if ( GenericParameter.class.isInstance( argument ) ) {
				areAllArgumentsDynamic = false;
				break;
			}
		}

		if ( areAllArgumentsDynamic ) {
			sqlAppender.appendSql( "cast(" );
			super.render( sqlAppender, sqlAstArguments, walker, sessionFactory );
			sqlAppender.appendSql( " as varchar(32672))" );
		}
		else {
			super.render( sqlAppender, sqlAstArguments, walker, sessionFactory );
		}
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression sqlAstArgument,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( "varchar(" );
		super.renderArgument( sqlAppender, sqlAstArgument, walker, sessionFactory );
		sqlAppender.appendSql( ")" );
	}

}
