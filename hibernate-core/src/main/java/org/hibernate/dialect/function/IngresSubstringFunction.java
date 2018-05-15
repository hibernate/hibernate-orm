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
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class IngresSubstringFunction
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {
	/**
	 * Singleton access
	 */
	public static final IngresSubstringFunction INSTANCE = new IngresSubstringFunction();

	public IngresSubstringFunction() {
		super(
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING ),
				StandardArgumentsValidators.between( 2, 3 )
		);
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType resolvedReturnType) {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		// substring(?1 FROM ?2 [FOR ?3])
		sqlAppender.appendSql( "substring(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( " from " );
		sqlAstArguments.get( 1 ).accept( walker );
		if ( sqlAstArguments.size() == 3 ) {
			sqlAppender.appendSql( " for " );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.appendSql( ")" );
	}
}
