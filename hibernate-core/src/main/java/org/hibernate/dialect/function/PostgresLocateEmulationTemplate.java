/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.Arrays;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSelfRenderingFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.produce.function.spi.StandardArgumentsValidators;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmTupleExpression;
import org.hibernate.query.sqm.tree.expression.function.AbstractSqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.Type;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Emulation of <tt>locate()</tt> on PostgreSQL
 *
 * @author Gavin King
 */
public class PostgresLocateEmulationTemplate extends AbstractSqmFunctionTemplate {
	private static final SqmExpression IN_LITERAL = new LiteralStringSqmExpression( " in ", null );
	private static final SqmExpression MINUS_THEN_PAREN = new LiteralStringSqmExpression( "-1)", null );
	private static final ArgumentsValidator ARGUMENTS_VALIDATOR = StandardArgumentsValidators.count( 2 );


	public PostgresLocateEmulationTemplate() {
		super( StandardArgumentsValidators.between( 2, 3 ) );
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {

		if ( arguments.size() == 2  ){
			return build3ArgVariation( arguments, impliedResultType );
		}
		else {
			return build2ArgVariation( arguments, impliedResultType );
		}


	}

	private SqmExpression build3ArgVariation(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		final boolean threeArgs = arguments.size() > 2;
		final SqmExpression pattern = arguments.get( 0 );
		final SqmExpression string = arguments.get( 1 );
		final SqmExpression start = threeArgs ? arguments.get( 2 ) : null;

		// (position( $pattern in substring($string, $start) ) + $start-1)
		return null;
	}

	private SqmExpression build2ArgVariation(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new PositionFunction(
				arguments.get( 0 ),
				arguments.get( 1 ),
				impliedResultType
		);
	}

	@Override
	public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		final boolean threeArgs = args.size() > 2;
		final Object pattern = args.get( 0 );
		final Object string = args.get( 1 );
		final Object start = threeArgs ? args.get( 2 ) : null;

		final StringBuilder buf = new StringBuilder();
		if (threeArgs) {
			buf.append( '(' );
		}
		buf.append( "position(" ).append( pattern ).append( " in " );
		if (threeArgs) {
			buf.append( "substring(");
		}
		buf.append( string );
		if (threeArgs) {
			buf.append( ", " ).append( start ).append( ')' );
		}
		buf.append( ')' );
		if (threeArgs) {
			buf.append( '+' ).append( start ).append( "-1)" );
		}
		return buf.toString();
	}

	public static class PositionFunction
			extends SelfRenderingSqmFunction
			implements SelfRenderingFunctionSupport, SqlAstFunctionProducer {
		public PositionFunction(
				SqmExpression pattern,
				SqmExpression string,
				AllowableFunctionReturnType resultType) {
			super(
					null,
				   Arrays.asList( pattern, string ),
				   resultType == null ? StandardSpiBasicTypes.STRING : resultType
			);
		}

		@Override
		public SelfRenderingFunctionSupport getRenderingSupport() {
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void render(
				SqlAppender sqlAppender,
				List<Expression> sqlAstArguments,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			// position( $pattern in $string )
			sqlAppender.appendSql( "position(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " in " );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")" );
		}
	}

}
