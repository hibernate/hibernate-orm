/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.internal;

import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link SQLExceptionConverter} that delegates to a chain of
 * {@link SQLExceptionConversionDelegate}.
 *
 * @author Steve Ebersole
 */
public class StandardSQLExceptionConverter implements SQLExceptionConverter {

	private final List<SQLExceptionConversionDelegate> delegates;

	public StandardSQLExceptionConverter(SQLExceptionConversionDelegate... delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	/**
	 * @deprecated use {@link #StandardSQLExceptionConverter(SQLExceptionConversionDelegate...)}
	 */
	@Deprecated(since = "6.0")
	public StandardSQLExceptionConverter() {
		delegates = new ArrayList<>();
	}

	/**
	 * Add a delegate.
	 *
	 * @deprecated use {@link #StandardSQLExceptionConverter(SQLExceptionConversionDelegate...)}
	 */
	@Deprecated(since = "6.0")
	public void addDelegate(@Nullable SQLExceptionConversionDelegate delegate) {
		if ( delegate != null ) {
			this.delegates.add( delegate );
		}
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		for ( SQLExceptionConversionDelegate delegate : delegates ) {
			final JDBCException jdbcException = delegate.convert( sqlException, message, sql );
			if ( jdbcException != null ) {
				return jdbcException;
			}
		}
		return new GenericJDBCException( message, sqlException, sql );
	}
}
