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
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.LiteralHelper;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.function.SqmNonStandardFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Emulation of <tt>locate()</tt> on using Dialect's position and substring
 * functions for Dialects which do not support locate or only support the
 * 2-argument form
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LocateEmulationUsingPositionAndSubstring
		extends AbstractSqmFunctionTemplate {
	private final SqmFunctionProducer positionFunctionProducer;
	private final SqmFunctionProducer substringFunctionProducer;

	public LocateEmulationUsingPositionAndSubstring() {
		this(
				(type, arguments, queryEngine) -> {
					final SqmFunctionTemplate template = queryEngine.getSqmFunctionRegistry().findFunctionTemplate( "substring" );
					if ( template == null ) {
						//noinspection unchecked
						return new SqmSubstringFunction(
								arguments.get( 1 ),
								arguments.get( 2 ),
								null,
								(BasicValuedExpressableType) type,
								queryEngine.getCriteriaBuilder()
						);
					}
					else {
						return template.makeSqmFunctionExpression(
								arguments,
								type,
								queryEngine
						);
					}
				}
		);
	}

	public LocateEmulationUsingPositionAndSubstring(SqmFunctionProducer substringFunctionProducer) {
		this(
				(type, arguments, queryEngine) -> {
					final SqmFunctionTemplate template = queryEngine.getSqmFunctionRegistry().findFunctionTemplate( "substring" );
					if ( template == null ) {
						return new PositionFunction(
								arguments.get( 0 ),
								arguments.get( 1 ),
								type,
								queryEngine.getCriteriaBuilder()
						);
					}
					else {
						return template.makeSqmFunctionExpression(
								arguments,
								type,
								queryEngine
						);
					}
				},
				substringFunctionProducer
		);
	}

	public LocateEmulationUsingPositionAndSubstring(
			SqmFunctionProducer positionFunctionProducer,
			SqmFunctionProducer substringFunctionProducer) {
		super( StandardArgumentsValidators.between( 2, 3 ) );

		this.positionFunctionProducer = positionFunctionProducer;
		this.substringFunctionProducer = substringFunctionProducer;
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType,
			QueryEngine queryEngine) {

		if ( arguments.size() == 2 ) {
			return build3ArgVariation( arguments, impliedResultType, queryEngine );
		}
		else {
			return build2ArgVariation( arguments, impliedResultType, queryEngine );
		}


	}

	private SqmExpression build3ArgVariation(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType,
			QueryEngine queryEngine) {

		final SqmExpression<?> pattern = arguments.get( 0 );
		final SqmExpression<?> string = arguments.get( 1 );
		final SqmExpression<?> start = arguments.get( 2 );

		// (position( $pattern in substring($string, $start) ) + $start-1)

		final SqmExpression substringCall = substringFunctionProducer.produce(
				StandardSpiBasicTypes.INTEGER,
				Arrays.asList( string, start ),
				queryEngine
		);


		final SqmExpression positionCall = positionFunctionProducer.produce(
				StandardSpiBasicTypes.INTEGER,
				Arrays.asList( pattern, substringCall ),
				queryEngine
		);

		//noinspection unchecked
		final SqmExpression startMinusOne = new SqmBinaryArithmetic(
				BinaryArithmeticOperator.SUBTRACT,
				start,
				LiteralHelper.integerLiteral( 1, queryEngine ),
				StandardSpiBasicTypes.INTEGER,
				queryEngine.getCriteriaBuilder()
		);


		//noinspection unchecked
		final SqmExpression positionPluStartMinusOne = new SqmBinaryArithmetic(
				BinaryArithmeticOperator.ADD,
				positionCall,
				startMinusOne,
				StandardSpiBasicTypes.INTEGER,
				queryEngine.getCriteriaBuilder()
		);

		return positionPluStartMinusOne;
	}

	private SqmExpression build2ArgVariation(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType,
			QueryEngine queryEngine) {
		return new PositionFunction(
				arguments.get( 0 ),
				arguments.get( 1 ),
				impliedResultType,
				queryEngine.getCriteriaBuilder()
		);
	}

	public static class PositionFunction
			extends SelfRenderingSqmFunction
			implements SelfRenderingFunctionSupport, SqlAstFunctionProducer, SqmNonStandardFunction {
		public PositionFunction(
				SqmExpression<?> pattern,
				SqmExpression<?> string,
				AllowableFunctionReturnType resultType,
				NodeBuilder nodeBuilder) {
			super(
					Arrays.asList( pattern, string ),
					resultType == null ? StandardSpiBasicTypes.STRING : resultType,
					nodeBuilder
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
				List<SqlAstNode> sqlAstArguments,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			// position( $pattern in $string )
			sqlAppender.appendSql( "position(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " in " );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")" );
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return getExpressableType().getJavaTypeDescriptor();
		}
	}

}
