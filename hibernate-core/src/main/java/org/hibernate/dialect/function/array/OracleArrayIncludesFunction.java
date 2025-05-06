/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayIncludesFunction extends AbstractArrayIncludesFunction {

	public OracleArrayIncludesFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		if ( nullable ) {
			final String arrayTypeName = DdlTypeHelper.getTypeName(
					haystackExpression.getExpressionType(),
					walker.getSessionFactory().getTypeConfiguration()
					);
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.append( "_includes(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ',' );
			sqlAppender.append( "1" );
			sqlAppender.append( ")>0" );
		}
		else {
			sqlAppender.append( " not exists ((select column_value from table (" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ")) minus (select column_value from table(" );
			haystackExpression.accept( walker );
			sqlAppender.append( ")))" );
		}

	}
}
