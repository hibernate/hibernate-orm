/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcType;

/**
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLCastingJsonArrayJdbcType.
 */
public class GaussDBCastingJsonArrayJdbcType extends JsonArrayJdbcType {

	private final boolean jsonb;

	public GaussDBCastingJsonArrayJdbcType(JdbcType elementJdbcType, boolean jsonb) {
		super( elementJdbcType );
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
