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
 * Implement the array contains function by using {@code unnest}.
 */
public class ArrayContainsUnnestFunction extends AbstractArrayContainsFunction {

	public ArrayContainsUnnestFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
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
				walker.render( haystackExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
				sqlAppender.append( " is not null and " );
			}
			if ( ArrayHelper.isNullable( needleExpression ) ) {
				walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
				sqlAppender.append( " is not null and " );
			}
			if ( !nullable ) {
				sqlAppender.append( "not exists(select 1 from unnest(" );
				walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
				sqlAppender.append( ") t(i) where t.i is null) and " );
			}
			sqlAppender.append( "not exists(select * from unnest(" );
			walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ") except select * from unnest(" );
			walker.render( haystackExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ")))" );
		}
		else {
			sqlAppender.append( "exists(select 1 from unnest(" );
			walker.render( haystackExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ") t(i) where t.i" );
			if ( nullable ) {
				sqlAppender.append( " is not distinct from " );
			}
			else {
				sqlAppender.append( '=' );
			}
			walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ")" );
		}
	}

}
