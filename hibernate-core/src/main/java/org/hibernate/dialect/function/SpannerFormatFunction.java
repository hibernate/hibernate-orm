/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * The format function for Spanner.
 * It uses FORMAT_DATE for temporal type date and FORMAT_TIMESTAMP for temporal type time and timestamp.
 */
public class SpannerFormatFunction extends FormatFunction {
	public SpannerFormatFunction(TypeConfiguration typeConfiguration) {
		super("format_timestamp", true, true, false, typeConfiguration);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		var datetime = (Expression) sqlAstArguments.get( 0 );
		var format = sqlAstArguments.get( 1 );
		var temporalType = getSqlTemporalType( datetime.getExpressionType() );
		switch ( temporalType ) {
			case DATE -> sqlAppender.appendSql( "format_date(" );
			case TIME, TIMESTAMP -> sqlAppender.appendSql( "format_timestamp(" );
			default -> throw new IllegalArgumentException( "Unsupported temporal type: " + temporalType );
		}
		format.accept( walker );
		sqlAppender.append( ',' );
		datetime.accept( walker );
		sqlAppender.append( ')' );
	}
}
