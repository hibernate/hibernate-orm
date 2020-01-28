/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

/**
 * @author Gavin King
 */
public class CastFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private Dialect dialect;

	public CastFunction(Dialect dialect) {
		super(
				"cast",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 )
		);
		this.dialect = dialect;
	}

	@Override
	public void render(SqlAppender sqlAppender, List<SqlAstNode> arguments, SqlAstWalker walker) {
		CastTarget targetType = (CastTarget) arguments.get(1);
		Expression arg = (Expression) arguments.get(0);

		CastType to = CastType.from( targetType.getExpressionType().getBasicType() );
		//TODO: generalize this to things which aren't BasicValuedMappings
		CastType from = CastType.from( ( (BasicValuedMapping) arg.getExpressionType() ).getBasicType() );

		String cast = dialect.castPattern( from, to );

		new PatternRenderer( cast ).render( sqlAppender, arguments, walker );
	}

//	@Override
//	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			AllowableFunctionReturnType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		SqmCastTarget<?> targetType = (SqmCastTarget<?>) arguments.get(1);
//		SqmExpression<?> arg = (SqmExpression<?>) arguments.get(0);
//
//		CastType to = CastType.from( targetType.getType() );
//		CastType from = CastType.from( arg.getNodeType() );
//
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder( "cast", dialect.castPattern( from, to ) )
//				.setExactArgumentCount( 2 )
//				.setReturnTypeResolver( useArgType( 2 ) )
//				.descriptor()
//				.generateSqmExpression(
//						arguments,
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	@Override
	public String getArgumentListSignature() {
		return "(arg as Type)";
	}

}
