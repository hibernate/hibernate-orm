/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.internal;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;
import org.hibernate.sql.spi.SqlAppender;

/// Built-in standard temporal-format translation.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardTemporalFormatSupport implements TemporalFormatSupport {
	private static final TemporalFormatSupport INSTANCE = new StandardTemporalFormatSupport();

	private StandardTemporalFormatSupport() {
	}

	public static TemporalFormatSupport instance() {
		return INSTANCE;
	}

	@Override
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( OracleDialect.datetimeFormat( format, true, false ).result() );
	}
}
