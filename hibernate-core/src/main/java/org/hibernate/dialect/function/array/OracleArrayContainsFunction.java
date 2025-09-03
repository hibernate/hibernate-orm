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

public class OracleArrayContainsFunction extends AbstractArrayContainsFunction {

	public OracleArrayContainsFunction(boolean nullable, TypeConfiguration typeConfiguration) {
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
			log.deprecatedArrayContainsWithArray();
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
				sqlAppender.append( " exists (select 1 from (table (" );
				needleExpression.accept( walker );
				sqlAppender.append( ") join (table (" );
				haystackExpression.accept( walker );
				sqlAppender.append( ")) using (column_value)))" );
			}
		}
		else {
			needleExpression.accept( walker );
			sqlAppender.append( " in (select column_value from table(" );
			haystackExpression.accept( walker );
			sqlAppender.append( "))" );
		}
	}
}
