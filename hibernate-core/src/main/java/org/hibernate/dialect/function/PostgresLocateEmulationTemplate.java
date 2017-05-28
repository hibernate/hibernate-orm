/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmTupleExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Emulation of <tt>locate()</tt> on PostgreSQL
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PostgresLocateEmulationTemplate extends AbstractSqmFunctionTemplate {
	public static final SqmExpression ONE = new LiteralIntegerSqmExpression( 1 );


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
		final SqmExpression pattern = arguments.get( 0 );
		final SqmExpression string = arguments.get( 1 );
		final SqmExpression start = arguments.get( 2 );

		final ExpressableType resolvedIntResultType = impliedResultType != null
				? impliedResultType
				: start.getExpressionType() == null ? ONE.getExpressionType() : start.getExpressionType();

		// (position( $pattern in substring($string, $start) ) + $start-1)

		final SqmGenericFunction substringCall = new SqmGenericFunction(
				"substring",
				impliedResultType,
				Arrays.asList( string, start )
		);

		final PositionFunction positionCall = new PositionFunction( pattern, substringCall, impliedResultType );

		final SqmExpression startMinusOne = new BinaryArithmeticSqmExpression(
				BinaryArithmeticSqmExpression.Operation.SUBTRACT,
				start,
				ONE,
				resolvedIntResultType
		);


		final SqmExpression positionPluStartMinusOne = new BinaryArithmeticSqmExpression(
				BinaryArithmeticSqmExpression.Operation.ADD,
				positionCall,
				startMinusOne,
				resolvedIntResultType
		);

		return new SqmTupleExpression( positionPluStartMinusOne );
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
