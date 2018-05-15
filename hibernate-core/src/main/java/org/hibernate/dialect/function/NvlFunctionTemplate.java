/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmNonStandardFunction;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Definition of an Oracle-style `nvl` function
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NvlFunctionTemplate
		extends AbstractSqmFunctionTemplate {
	/**
	 * Singleton access
	 */
	public static final NvlFunctionTemplate INSTANCE = new NvlFunctionTemplate();
	public static final String NAME = "nvl";

	public NvlFunctionTemplate() {
		super( StandardArgumentsValidators.exactly( 2 ) );
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new SqmNvlFunction(
				arguments.get( 0 ),
				arguments.get( 1 ),
				impliedResultType
		);
	}

	public static class SqmNvlFunction
			extends SelfRenderingSqmFunction
			implements SelfRenderingFunctionSupport, SqlAstFunctionProducer, SqmNonStandardFunction {
		public SqmNvlFunction(
				SqmExpression arg1,
				SqmExpression arg2,
				AllowableFunctionReturnType impliedResultType) {
			super( Arrays.asList( arg1, arg2 ), impliedResultType );
		}

		@Override
		public String getFunctionName() {
			return NAME;
		}

		@Override
		public boolean hasArguments() {
			return true;
		}

		@Override
		public String asLoggableText() {
			return String.format(
					Locale.ROOT,
					"%s( %s, %s )",
					NAME,
					getSqmArguments().get( 0 ),
					getSqmArguments().get( 1 )
			);
		}

		@Override
		@SuppressWarnings("unchecked")
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

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return getExpressableType().getJavaTypeDescriptor();
		}
	}
}
