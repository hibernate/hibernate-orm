/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.spi.AbstractSelfRenderingFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.spi.BasicType;

/**
 * Emulation of <tt>locate()</tt> on Sybase
 *
 * @author Nathan Moon
 */
public class SybaseLocateEmulationFunctionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {

	public SybaseLocateEmulationFunctionTemplate() {
		super( StandardArgumentsValidators.between( 2, 3 ) );
	}

	public SybaseLocateEmulationFunctionTemplate(BasicType basicType) {
		super(
				StandardFunctionReturnTypeResolvers.invariant( basicType ),
				StandardArgumentsValidators.between( 2, 3 )
		);
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		final int argCount = sqlAstArguments.size();
		assert argCount == 2 || argCount == 3;

		sqlAppender.appendSql( "charindex(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ", " );

		if ( argCount == 3 ) {
			sqlAppender.appendSql( "right(" );
		}

		sqlAstArguments.get( 1 ).accept( walker );

		if ( argCount == 3 ) {
			sqlAppender.appendSql( ", char_length(" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")-(" );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.appendSql( "-1))" );
		}
	}

}
