/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;

/**
 * A helper to centralize conversion of {@link SQLException}s to {@link JDBCException}s.
 * <p>
 * Used while querying JDBC metadata during bootstrapping
 *
 * @author Steve Ebersole
 */
public class BasicSQLExceptionConverter {

	/**
	 * Singleton access
	 */
	public static final BasicSQLExceptionConverter INSTANCE = new BasicSQLExceptionConverter();

	/**
	 * Message
	 */

	private static final SQLExceptionConverter CONVERTER = new StandardSQLExceptionConverter(
			new SQLStateConversionDelegate(() -> sqle ->"???" )
	);

	/**
	 * Perform a conversion.
	 *
	 * @param sqlException The exception to convert.
	 * @return The converted exception.
	 */
	public JDBCException convert(SQLException sqlException) {
		return CONVERTER.convert( sqlException, "Unable to query java.sql.DatabaseMetaData", null );
	}

}
