/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.sql.Types;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * ANSI SQL-inspired {@code cast()} function, where the target types
 * are enumerated by {@link CastType}, and portability is achieved
 * by delegating to {@link Dialect#castPattern(CastType, CastType)}.
 *
 * @author Gavin King
 */
public class CastFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final CastType booleanCastType;

	public CastFunction(Dialect dialect, int preferredSqlTypeCodeForBoolean) {
		super(
				StandardFunctions.CAST,
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 ),
				StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
		);
		this.dialect = dialect;
		this.booleanCastType = getBooleanCastType( preferredSqlTypeCodeForBoolean );
	}

	private CastType getBooleanCastType(int preferredSqlTypeCodeForBoolean) {
		switch ( preferredSqlTypeCodeForBoolean ) {
			case Types.BIT:
			case Types.SMALLINT:
			case Types.TINYINT:
				return CastType.INTEGER_BOOLEAN;
		}
		return CastType.BOOLEAN;
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, SqlAstTranslator<?> walker) {
		final Expression source = (Expression) arguments.get( 0 );
		final JdbcMapping sourceMapping = source.getExpressionType().getJdbcMappings().get( 0 );
		final CastType sourceType = getCastType( sourceMapping );

		final CastTarget castTarget = (CastTarget) arguments.get( 1 );
		final JdbcMapping targetJdbcMapping = castTarget.getExpressionType().getJdbcMappings().get( 0 );
		final CastType targetType = getCastType( targetJdbcMapping );

		String cast = dialect.castPattern( sourceType, targetType );

		new PatternRenderer( cast ).render( sqlAppender, arguments, walker );
	}

	private CastType getCastType(JdbcMapping sourceMapping) {
		final CastType castType = sourceMapping.getCastType();
		return castType == CastType.BOOLEAN ? booleanCastType : castType;
	}

//	@Override
//	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			ReturnableType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		SqmCastTarget<?> targetType = (SqmCastTarget<?>) arguments.get(1);
//		SqmExpression<?> arg = (SqmExpression<?>) arguments.get(0);
//
//		CastType to = CastType.from( targetType.getType() );
//		CastType from = CastType.from( arg.getNodeType() );
//
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder( StandardFunctions.CAST, dialect.castPattern( from, to ) )
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
