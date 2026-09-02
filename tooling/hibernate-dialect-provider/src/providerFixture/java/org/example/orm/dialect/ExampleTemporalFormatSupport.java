/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;
import org.hibernate.sql.spi.SqlAppender;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// External-provider example of nonstandard temporal-format translation.
///
/// @author Steve Ebersole
public enum ExampleTemporalFormatSupport implements TemporalFormatSupport {
	INSTANCE;

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( "fixture_format[" );
		appender.appendSql( format );
		appender.appendSql( ']' );
	}
}
