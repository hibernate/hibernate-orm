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
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.produce.function.spi.SqmFunctionRegistryAware;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.function.SqmNonStandardFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
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
		extends AbstractSqmFunctionTemplate
		implements SqmFunctionRegistryAware {
	public static final SqmExpression ONE = new SqmLiteralInteger( 1 );

	private final SqmFunctionProducer positionFunctionProducer;
	private final SqmFunctionProducer substringFunctionProducer;

	private SqmFunctionRegistry registry;

	public LocateEmulationUsingPositionAndSubstring() {
		this(
				(registry, type, arguments) -> {
					final SqmFunctionTemplate template = registry.findFunctionTemplate( "substring" );
					if ( template == null ) {
						return new SqmSubstringFunction(
								(BasicValuedExpressableType) type,
								arguments.get( 1 ),
								arguments.get( 2 ),
								null
						);
					}
					else {
						return template.makeSqmFunctionExpression( arguments, type );
					}
				}
		);
	}

	public LocateEmulationUsingPositionAndSubstring(SqmFunctionProducer substringFunctionProducer) {
		this(
				(registry, type, arguments) -> {
					final SqmFunctionTemplate template = registry.findFunctionTemplate( "substring" );
					if ( template == null ) {
						return new PositionFunction(
								arguments.get( 0 ),
								arguments.get( 1 ),
								type
						);
					}
					else {
						return template.makeSqmFunctionExpression( arguments, type );
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
			AllowableFunctionReturnType impliedResultType) {

		if ( arguments.size() == 2 ) {
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
				: start.getExpressableType() == null ? ONE.getExpressableType() : start.getExpressableType();

		// (position( $pattern in substring($string, $start) ) + $start-1)

		final SqmExpression substringCall = substringFunctionProducer.produce(
				registry,
				impliedResultType,
				Arrays.asList( string, start )
		);

		final SqmExpression positionCall = positionFunctionProducer.produce(
				registry,
				impliedResultType,
				Arrays.asList( pattern, substringCall )
		);

		final SqmExpression startMinusOne = new SqmBinaryArithmetic(
				SqmBinaryArithmetic.Operation.SUBTRACT,
				start,
				ONE,
				resolvedIntResultType
		);


		final SqmExpression positionPluStartMinusOne = new SqmBinaryArithmetic(
				SqmBinaryArithmetic.Operation.ADD,
				positionCall,
				startMinusOne,
				resolvedIntResultType
		);

		return new SqmTuple( positionPluStartMinusOne );
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
	public void injectRegistry(SqmFunctionRegistry registry) {
		this.registry = registry;
	}

	public static class PositionFunction
			extends SelfRenderingSqmFunction
			implements SelfRenderingFunctionSupport, SqlAstFunctionProducer, SqmNonStandardFunction {
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

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return getExpressableType().getJavaTypeDescriptor();
		}
	}

}
