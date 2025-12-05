/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.internal;

import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;


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

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		for ( var delegate : delegates ) {
			final var jdbcException = delegate.convert( sqlException, message, sql );
			if ( jdbcException != null ) {
				return jdbcException;
			}
		}
		return new GenericJDBCException( message, sqlException, sql );
	}
}
