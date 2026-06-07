/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.UUID;

/**
 * HSQLDB json_array function.
 */
public class HSQLJsonArrayFunction extends JsonArrayFunction {

	public HSQLJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		renderJsonWriteExpression( sqlAppender, value, walker );
	}

	static void renderJsonWriteExpression(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( value instanceof Literal literal && literal.getLiteralValue() == null ) {
			sqlAppender.appendSql( "cast(null as int)" );
		}
		else {
			final JdbcMappingContainer expressionType = ( (Expression) value ).getExpressionType();
			final int sqlTypeCode = expressionType.getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
			switch ( sqlTypeCode ) {
				case UUID -> {
					sqlAppender.append( "regexp_replace(lower(cast(" );
					value.accept( walker );
					sqlAppender.append( " as varchar(36))),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','$1-$2-$3-$4-$5')" );
				}
				case TIMESTAMP -> {
					sqlAppender.append( "replace(to_char(" );
					value.accept( walker );
					sqlAppender.append( ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')" );
				}
				case TIMESTAMP_UTC -> {
					sqlAppender.append( "replace(to_char(" );
					value.accept( walker );
					sqlAppender.append( ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')||'Z'" );
				}
				default -> value.accept( walker );
			}
		}
	}
}
