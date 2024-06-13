/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same.
 */
public class H2ArrayContainsFunction extends AbstractArrayContainsFunction {

	private final int maximumArraySize;

	public H2ArrayContainsFunction(boolean nullable, int maximumArraySize, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
		final JdbcMappingContainer needleTypeContainer = needleExpression.getExpressionType();
		final JdbcMapping needleType = needleTypeContainer == null ? null : needleTypeContainer.getSingleJdbcMapping();
		if ( needleType == null || needleType instanceof BasicPluralType<?, ?> ) {
			LOG.deprecatedArrayContainsWithArray();
			sqlAppender.append( '(' );
			if ( ArrayHelper.isNullable( haystackExpression ) ) {
				haystackExpression.accept( walker );
				sqlAppender.append( " is not null and " );
			}
			if ( ArrayHelper.isNullable( needleExpression ) ) {
				needleExpression.accept( walker );
				sqlAppender.append( " is not null and " );
			}
			if ( !nullable ) {
				sqlAppender.append( "not array_contains(" );
				needleExpression.accept( walker );
				sqlAppender.append( ",null) and " );
			}
			sqlAppender.append( "not " );
			sqlAppender.append( "exists(select array_get(" );
			walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ",t.i) from system_range(1," );
			sqlAppender.append( Integer.toString( maximumArraySize ) );
			sqlAppender.append( ") t(i) where array_length(" );
			needleExpression.accept( walker );
			sqlAppender.append( ")>=t.i" );
			sqlAppender.append( " except " );
			sqlAppender.append( "select array_get(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ",t.i) from system_range(1," );
			sqlAppender.append( Integer.toString( maximumArraySize ) );
			sqlAppender.append( ") t(i) where array_length(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ")>=t.i))" );
		}
		else {
			sqlAppender.append( "array_contains(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ',' );
			needleExpression.accept( walker );
			sqlAppender.append( ')' );
		}
	}

}
