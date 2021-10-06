/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * @author Gavin King
 */
public class TrimFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;

	public TrimFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super(
				"trim",
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
		this.dialect = dialect;
	}

	@Override
	public void render(SqlAppender sqlAppender, List<SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		final TrimSpec specification = ( (TrimSpecification) sqlAstArguments.get( 0 ) ).getSpecification();
		final Character trimCharacter = ( (QueryLiteral<Character>) sqlAstArguments.get( 1 ) ).getLiteralValue();
		final Expression sourceExpr = (Expression) sqlAstArguments.get( 2 );

		String trim = dialect.trimPattern( specification, trimCharacter );

		new PatternRenderer( trim ).render( sqlAppender, Collections.singletonList( sourceExpr ), walker );
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	public <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			AllowableFunctionReturnType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
//		final char trimCharacter = ( (SqmLiteral<Character>) arguments.get( 1 ) ).getLiteralValue();
//		final SqmExpression sourceExpr = (SqmExpression) arguments.get( 2 );
//
//		String trim = dialect.trimPattern( specification, trimCharacter );
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder( "trim", trim )
//				.setInvariantType( StandardBasicTypes.STRING )
//				.setExactArgumentCount( 1 )
//				.descriptor() //TODO: we could cache the 6 variations here
//				.generateSqmExpression(
//						Collections.singletonList( sourceExpr ),
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	@Override
	public String getArgumentListSignature() {
		return "([[{leading|trailing|both} ][arg0 ]from] arg1)";
	}
}
