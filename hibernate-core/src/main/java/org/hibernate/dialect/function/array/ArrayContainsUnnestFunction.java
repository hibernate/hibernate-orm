/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Subclassable `array_contains` descriptor implemented using `unnest`.
@SPI({ USE, IMPLEMENT })
public class ArrayContainsUnnestFunction extends AbstractArrayContainsFunction {

	@SPI(IMPLEMENT)
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
			warnAboutArrayContainsWithArrayArgument();
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
