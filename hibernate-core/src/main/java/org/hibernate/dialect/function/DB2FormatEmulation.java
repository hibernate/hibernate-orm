/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * DB2's varchar_format() can't handle quoted literal strings in
 * the format pattern. So just split the pattern into bits, call
 * varcharformat() on the odd-numbered bits, and concatenate all
 * the nonempty bits at the end.
 *
 * @author Gavin King
 */
public class DB2FormatEmulation extends FormatFunction {

	public DB2FormatEmulation(TypeConfiguration typeConfiguration) {
		super(
				"varchar_format",
				false,
				false,
				typeConfiguration
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression datetime = (Expression) arguments.get( 0 );
		sqlAppender.appendSql( "varchar_format(" );
		// Times need to be wrapped into a timestamp to be able to use formatting
		if ( TypeConfiguration.getSqlTemporalType( datetime.getExpressionType() ) == TemporalType.TIME ) {
			sqlAppender.appendSql( "timestamp(current_date," );
			datetime.accept( walker );
			sqlAppender.appendSql( ")" );
		}
		else {
			datetime.accept( walker );
		}
		sqlAppender.appendSql( "," );
		arguments.get( 1 ).accept( walker );
		sqlAppender.appendSql( ")" );
	}
}
