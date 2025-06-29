/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.jboss.logging.Logger;

/**
 * Standard implementation of {@link RefCursorSupport}
 *
 * @author Steve Ebersole
 */
public class StandardRefCursorSupport implements RefCursorSupport {
	private static final Logger log = Logger.getLogger( StandardRefCursorSupport.class );

	private final JdbcServices jdbcServices;

	public StandardRefCursorSupport(JdbcServices jdbcServices) {
		this.jdbcServices = jdbcServices;
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		try {
			statement.registerOutParameter( position, refCursorTypeCode() );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper()
					.convert( e, "Error registering REF_CURSOR parameter [" + position + "]" );
		}
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		try {
			statement.registerOutParameter( name, refCursorTypeCode() );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper()
					.convert( e, "Error registering REF_CURSOR parameter [" + name + "]" );
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		try {
			return statement.getObject( position, ResultSet.class );
		}
		catch (Exception e) {
			throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + position + "]", e );
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		try {
			return statement.getObject( name, ResultSet.class );
		}
		catch (Exception e) {
			throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + name + "]", e );
		}
	}

	/**
	 * Does this JDBC metadata indicate that the driver defines REF_CURSOR support?
	 *
	 * @param meta The JDBC metadata
	 *
	 * @return {@code true} if the metadata indicates that the driver defines REF_CURSOR support
	 */
	public static boolean supportsRefCursors(DatabaseMetaData meta) {
		try {
			final boolean mightSupportIt = meta.supportsRefCursors();
			// Some databases cheat and don't actually support it correctly: add some additional checks.
			if ( mightSupportIt ) {
				if ( "Oracle JDBC driver".equals( meta.getDriverName() ) && meta.getDriverMajorVersion() < 19 ) {
					return false;
				}
			}
			return mightSupportIt;
		}
		catch (Exception throwable) {
			//If the driver is not compatible with the Java 8 contract, the method might not exit.
			log.debug( "Unexpected error trying to gauge level of JDBC REF_CURSOR support : " + throwable.getMessage() );
			return false;
		}
	}

	private int refCursorTypeCode() {
		return Types.REF_CURSOR;
	}

}
