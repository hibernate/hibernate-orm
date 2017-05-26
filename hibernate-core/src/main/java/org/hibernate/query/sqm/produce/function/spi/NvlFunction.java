/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * Emulation of <tt>coalesce()</tt> on Oracle, using multiple <tt>nvl()</tt> calls
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NvlFunction extends AbstractSelfRenderingFunctionTemplate {
	public NvlFunction() {
		super( null, null );
	}

	@Override
	public SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new SelfRenderingFunctionSupportImpl();
	}

	private static class SelfRenderingFunctionSupportImpl implements SelfRenderingFunctionSupport {
		@Override
		public void render(
				SqlAppender sqlAppender,
				List<Expression> sqlAstArguments,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( "nvl(" );
			boolean firstPass = true;
			for ( Expression sqlAstArgument : sqlAstArguments ) {
				if ( !firstPass ) {
					sqlAppender.appendSql( "," );
				}
				sqlAstArgument.accept( walker );
				if ( firstPass ) {
					firstPass = false;
				}
			}
			sqlAppender.appendSql( ")" );
		}
	}
}
