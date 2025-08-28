/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.dialect.function.CastFunction.renderCastArrayToString;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

public class CastingConcatFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final String concatOperator;
	private final String concatArgumentCastType;
	private final boolean needsCastWrapper;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public CastingConcatFunction(
			Dialect dialect,
			String concatOperator,
			boolean needsCastWrapper,
			SqlAstNodeRenderingMode argumentRenderingMode,
			TypeConfiguration typeConfiguration) {
		super(
				"concat",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, STRING )
		);
		this.dialect = dialect;
		this.concatOperator = concatOperator;
		this.needsCastWrapper = needsCastWrapper;
		this.argumentRenderingMode = argumentRenderingMode;
		this.concatArgumentCastType = typeConfiguration.getDdlTypeRegistry().getDescriptor( SqlTypes.VARCHAR )
				.getCastTypeName(
						Size.nil(),
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						typeConfiguration.getDdlTypeRegistry()
				);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Apache Derby and DB2 add up the sizes of operands for concat operations and has a limit of 4000/32k until
		// it changes the data type to long varchar, at which point problems start arising, because a long varchar
		// can't be compared with a regular varchar for some reason.
		// For example, a comparison like `alias.varchar_column = cast(? as varchar(4000)) || 'a'` will fail,
		// because the result of `cast(? as varchar(4000)) || 'a'` is a long varchar.
		// For casts to unbounded types we usually use the maximum allowed size, which would be ~32k,
		// but concat operations lead to producing a long varchar, so we have to wrap the whole thing in a cast again
		if ( needsCastWrapper ) {
			sqlAppender.appendSql( "cast(" );
		}
		else {
			sqlAppender.appendSql( '(' );
		}
		renderAsString( sqlAppender, walker, (Expression) sqlAstArguments.get( 0 ) );
		for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
			sqlAppender.appendSql( concatOperator );
			renderAsString( sqlAppender, walker, (Expression) sqlAstArguments.get( i ) );
		}
		if ( needsCastWrapper ) {
			sqlAppender.appendSql( " as " );
			sqlAppender.appendSql( concatArgumentCastType );
		}
		sqlAppender.appendSql( ')' );
	}

	private void renderAsString(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression expression) {
		final JdbcMapping sourceMapping = expression.getExpressionType().getSingleJdbcMapping();
		final CastType sourceType = sourceMapping.getCastType();
		// No need to cast if we already have a string
		if ( sourceType == CastType.STRING ) {
			translator.render( expression, argumentRenderingMode );
		}
		else if ( sourceType == CastType.OTHER && sourceMapping.getJdbcType().isArray() ) {
			renderCastArrayToString( sqlAppender, expression, dialect, translator );
		}
		else {
			final String cast = dialect.castPattern( sourceType, CastType.STRING );
			new PatternRenderer( cast.replace( "?2", concatArgumentCastType ), argumentRenderingMode )
					.render( sqlAppender, Collections.singletonList( expression ), translator );
		}
	}
}
