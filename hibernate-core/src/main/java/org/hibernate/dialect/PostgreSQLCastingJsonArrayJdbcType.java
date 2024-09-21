/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLCastingJsonArrayJdbcType extends JsonArrayJdbcType {

	public static final PostgreSQLCastingJsonArrayJdbcType JSON_INSTANCE = new PostgreSQLCastingJsonArrayJdbcType( false );
	public static final PostgreSQLCastingJsonArrayJdbcType JSONB_INSTANCE = new PostgreSQLCastingJsonArrayJdbcType( true );

	private final boolean jsonb;

	public PostgreSQLCastingJsonArrayJdbcType(boolean jsonb) {
		this.jsonb = jsonb;
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
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
}
