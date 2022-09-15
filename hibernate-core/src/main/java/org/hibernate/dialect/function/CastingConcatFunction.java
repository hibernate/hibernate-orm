/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Collections;
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
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

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
				StandardFunctions.CONCAT,
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
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						null,
						null,
						null
				);
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
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
		final JdbcMapping sourceMapping = expression.getExpressionType().getJdbcMappings().get( 0 );
		// No need to cast if we already have a string
		if ( sourceMapping.getCastType() == CastType.STRING ) {
			translator.render( expression, argumentRenderingMode );
		}
		else {
			final String cast = dialect.castPattern( sourceMapping.getCastType(), CastType.STRING );
			new PatternRenderer( cast.replace( "?2", concatArgumentCastType ), argumentRenderingMode )
					.render( sqlAppender, Collections.singletonList( expression ), translator );
		}
	}
}
