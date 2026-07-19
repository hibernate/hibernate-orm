/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Special array contains implementation that uses the PostgreSQL {@code = any()} operator
 * for scalar needles. Using {@code = any()} instead of {@code @> cast(array[?] as ...)}
 * avoids incompatibilities between {@code text[]} and {@code varchar[]}.
 */
public class ArrayContainsOperatorFunction extends ArrayContainsUnnestFunction {

	public ArrayContainsOperatorFunction(boolean nullable, TypeConfiguration typeConfiguration) {
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
			if ( nullable ) {
				super.render( sqlAppender, sqlAstArguments, returnType, walker );
			}
			else {
				haystackExpression.accept( walker );
				sqlAppender.append( "@>" );
				needleExpression.accept( walker );
			}
		}
		else {
			if ( nullable ) {
				sqlAppender.append( "array_position(" );
				haystackExpression.accept( walker );
				sqlAppender.append( ',' );
				needleExpression.accept( walker );
				sqlAppender.append( ") is not null" );
			}
			else {
				needleExpression.accept( walker );
				sqlAppender.append( "=any(" );
				haystackExpression.accept( walker );
				sqlAppender.append( ')' );
			}
		}
	}
}
