/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.InjectService;

import org.jboss.logging.Logger;

/**
 * Standard implementation of RefCursorSupport
 *
 * @author Steve Ebersole
 */
public class StandardRefCursorSupport implements RefCursorSupport {
	private static final Logger log = Logger.getLogger( StandardRefCursorSupport.class );

	private JdbcServices jdbcServices;

	/**
	 * Hook for service registry to be able to inject JdbcServices
	 *
	 * @param jdbcServices The JdbcServices service
	 */
	@InjectService
	@SuppressWarnings("UnusedDeclaration")
	public void injectJdbcServices(JdbcServices jdbcServices) {
		this.jdbcServices = jdbcServices;
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		if ( jdbcServices.getExtractedMetaDataSupport().supportsRefCursors() ) {
			try {
				statement.registerOutParameter( position, refCursorTypeCode() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "Error registering REF_CURSOR parameter [" + position + "]" );
			}
		}
		else {
			try {
				jdbcServices.getDialect().registerResultSetOutParameter( statement, position );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "Error asking dialect to register ref cursor parameter [" + position + "]" );
			}
		}
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		if ( jdbcServices.getExtractedMetaDataSupport().supportsRefCursors() ) {
			try {
				statement.registerOutParameter( name, refCursorTypeCode() );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "Error registering REF_CURSOR parameter [" + name + "]" );
			}
		}
		else {
			try {
				jdbcServices.getDialect().registerResultSetOutParameter( statement, name );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, "Error asking dialect to register ref cursor parameter [" + name + "]" );
			}
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		if ( jdbcServices.getExtractedMetaDataSupport().supportsRefCursors() ) {
			try {
				return statement.getObject( position, ResultSet.class );
			}
			catch (Exception e) {
				throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + position + "]", e );
			}
		}
		else {
			try {
				return jdbcServices.getDialect().getResultSet( statement, position );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Error asking dialect to extract ResultSet from CallableStatement parameter [" + position + "]"
				);
			}
		}
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		if ( jdbcServices.getExtractedMetaDataSupport().supportsRefCursors() ) {
			try {
				return statement.getObject( name, ResultSet.class );
			}
			catch (Exception e) {
				throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + name + "]", e );
			}
		}
		else {
			try {
				return jdbcServices.getDialect().getResultSet( statement, name );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Error asking dialect to extract ResultSet from CallableStatement parameter [" + name + "]"
				);
			}
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
