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
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
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

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

public class CastingConcatFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final String concatOperator;
	private final String concatArgumentCastType;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public CastingConcatFunction(
			Dialect dialect,
			String concatOperator,
			SqlAstNodeRenderingMode argumentRenderingMode,
			TypeConfiguration typeConfiguration) {
		super(
				"concat",
				new ArgumentTypesValidator( StandardArgumentsValidators.min( 1 ), STRING ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
		this.dialect = dialect;
		this.concatOperator = concatOperator;
		this.argumentRenderingMode = argumentRenderingMode;
		this.concatArgumentCastType = dialect.getTypeName(
				SqlTypes.VARCHAR,
				dialect.getSizeStrategy().resolveSize(
						typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.VARCHAR ),
						typeConfiguration.getJavaTypeRegistry().getDescriptor( String.class ),
						null,
						null,
						null
				)
		);
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( '(' );
		renderAsString( sqlAppender, walker, (Expression) sqlAstArguments.get( 0 ) );
		for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
			sqlAppender.appendSql( concatOperator );
			renderAsString( sqlAppender, walker, (Expression) sqlAstArguments.get( i ) );
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
