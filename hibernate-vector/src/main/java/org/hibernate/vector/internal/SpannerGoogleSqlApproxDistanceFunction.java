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
 * Self-rendering SQM function descriptor for Cloud Spanner GoogleSQL approximate
 * distance functions ({@code APPROX_COSINE_DISTANCE}, {@code APPROX_EUCLIDEAN_DISTANCE},
 * and {@code APPROX_DOT_PRODUCT}).
 * <p>
 * Spanner's engine mandates a 3rd options argument formatted as a JSON literal.
 * This descriptor accepts 2 or 3 arguments:
 * <ul>
 *   <li>If 2 arguments are provided, emits default {@code options => JSON '{"num_leaves_to_search": 100}'}.</li>
 *   <li>If 3 arguments are provided and the 3rd is numeric {@code N}, emits {@code options => JSON '{"num_leaves_to_search": N}'}.</li>
 *   <li>If 3 arguments are provided and the 3rd is an expression/string literal, emits {@code options => JSON '...'}.</li>
 * </ul>
 */
public class SpannerGoogleSqlApproxDistanceFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String spannerFunctionName;

	public SpannerGoogleSqlApproxDistanceFunction(String functionName, TypeConfiguration typeConfiguration) {
		this( functionName, functionName.toUpperCase( Locale.ROOT ), typeConfiguration );
	}

	public SpannerGoogleSqlApproxDistanceFunction(String hqlName, String spannerFunctionName, TypeConfiguration typeConfiguration) {
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
		this.spannerFunctionName = spannerFunctionName;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( spannerFunctionName );
		sqlAppender.appendSql( '(' );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ", " );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.appendSql( ", options => " );

		if ( sqlAstArguments.size() == 3 ) {
			final SqlAstNode optionsNode = sqlAstArguments.get( 2 );
			if ( optionsNode instanceof Literal literal && literal.getLiteralValue() instanceof Number num ) {
				sqlAppender.appendSql( "JSON '{\"num_leaves_to_search\": " + num + "}'" );
			}
			else if ( optionsNode instanceof Literal literal && literal.getLiteralValue() instanceof String str ) {
				if ( str.trim().startsWith( "{" ) ) {
					sqlAppender.appendSql( "JSON '" + str.replace( "'", "''" ) + "'" );
				}
				else {
					sqlAppender.appendSql( "JSON '{\"num_leaves_to_search\": " + str.replace( "'", "''" ) + "}'" );
				}
			}
			else {
				optionsNode.accept( walker );
			}
		}
		else {
			sqlAppender.appendSql( "JSON '{\"num_leaves_to_search\": 100}'" );
		}
		sqlAppender.appendSql( ')' );
	}
}
