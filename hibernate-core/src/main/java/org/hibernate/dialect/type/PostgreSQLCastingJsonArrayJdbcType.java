/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLCastingJsonArrayJdbcType extends JsonArrayJdbcType {

	private final boolean jsonb;

	public PostgreSQLCastingJsonArrayJdbcType(JdbcType elementJdbcType, boolean jsonb) {
		super( elementJdbcType );
		this.jsonb = jsonb;
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as " );
		if ( jsonb ) {
			appender.append( "jsonb)" );
		}
		else {
			appender.append( "json)" );
		}
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return true;
	}
}
