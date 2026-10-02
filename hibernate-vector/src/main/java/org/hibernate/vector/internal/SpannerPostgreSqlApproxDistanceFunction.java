package org.hibernate.vector.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Self-rendering SQM function descriptor for Cloud Spanner PostgreSQL approximate
 * distance functions under the {@code spanner.*} schema namespace ({@code spanner.approx_cosine_distance},
 * {@code spanner.approx_euclidean_distance}, and {@code spanner.approx_dot_product}).
 * <p>
 * Spanner's engine mandates a 3rd options argument formatted as a JSON literal string.
 * This descriptor accepts 2 or 3 arguments:
 * <ul>
 *   <li>If 2 arguments are provided, emits default {@code options => '{"num_leaves_to_search": 100}'}.</li>
 *   <li>If 3 arguments are provided and the 3rd is numeric {@code N}, emits {@code options => '{"num_leaves_to_search": N}'}.</li>
 *   <li>If 3 arguments are provided and the 3rd is an expression/string literal, emits {@code options => ?3}.</li>
 * </ul>
 */
public class SpannerPostgreSqlApproxDistanceFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String pgFunctionName;

	public SpannerPostgreSqlApproxDistanceFunction(String functionName, TypeConfiguration typeConfiguration) {
		this( functionName, functionName.toLowerCase( Locale.ROOT ), typeConfiguration );
	}

	public SpannerPostgreSqlApproxDistanceFunction(String hqlName, String pgFunctionName, TypeConfiguration typeConfiguration) {
		super(
				hqlName,
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.between( 2, 3 ),
						VectorArgumentValidator.DISTANCE_INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				),
				(AbstractFunctionArgumentTypeResolver) (arguments, argumentIndex, converter) -> argumentIndex < 2
						? VectorArgumentTypeResolver.DISTANCE_INSTANCE.resolveFunctionArgumentType( arguments, argumentIndex, converter )
						: null
		);
		this.pgFunctionName = pgFunctionName.startsWith( "spanner." ) ? pgFunctionName : "spanner." + pgFunctionName;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( pgFunctionName );
		sqlAppender.appendSql( '(' );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ", " );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.appendSql( ", options => " );

		if ( sqlAstArguments.size() == 3 ) {
			final SqlAstNode optionsNode = sqlAstArguments.get( 2 );
			if ( optionsNode instanceof Literal literal && literal.getLiteralValue() instanceof Number num ) {
				sqlAppender.appendSql( "'{\"num_leaves_to_search\": " + num + "}'::jsonb" );
			}
			else {
				optionsNode.accept( walker );
				sqlAppender.appendSql( "::jsonb" );
			}
		}
		else {
			sqlAppender.appendSql( "'{\"num_leaves_to_search\": 100}'::jsonb" );
		}
		sqlAppender.appendSql( ')' );
	}
}
