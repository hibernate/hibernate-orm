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
				return (ResultSet) getResultSetByPositionMethod().invoke( statement, position, ResultSet.class );
			}
			catch (InvocationTargetException e) {
				if ( e.getTargetException() instanceof SQLException ) {
					throw jdbcServices.getSqlExceptionHelper().convert(
							(SQLException) e.getTargetException(),
							"Error extracting REF_CURSOR parameter [" + position + "]"
					);
				}
				else {
					throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + position + "]", e.getTargetException() );
				}
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
				return (ResultSet) getResultSetByNameMethod().invoke( statement, name, ResultSet.class );
			}
			catch (InvocationTargetException e) {
				if ( e.getTargetException() instanceof SQLException ) {
					throw jdbcServices.getSqlExceptionHelper().convert(
							(SQLException) e.getTargetException(),
							"Error extracting REF_CURSOR parameter [" + name + "]"
					);
				}
				else {
					throw new HibernateException( "Unexpected error extracting REF_CURSOR parameter [" + name + "]", e.getTargetException() );
				}
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
		// Standard JDBC REF_CURSOR support was not added until Java 8, so we need to use reflection to attempt to
		// access these fields/methods...
		try {
			return (Boolean) meta.getClass().getMethod( "supportsRefCursors" ).invoke( meta );
		}
		catch (NoSuchMethodException e) {
			log.trace( "JDBC DatabaseMetaData class does not define supportsRefCursors method..." );
		}
		catch (Exception e) {
			log.debug( "Unexpected error trying to gauge level of JDBC REF_CURSOR support : " + e.getMessage() );
		}
		return false;
	}


	private static Integer refCursorTypeCode;

	private int refCursorTypeCode() {
		if ( refCursorTypeCode == null ) {
			try {
				refCursorTypeCode = (Integer) Types.class.getField( "REF_CURSOR" ).get( null );
			}
			catch (NoSuchFieldException e) {
				throw new HibernateException( "java.sql.Types class does not define REF_CURSOR field..." );
			}
			catch (IllegalAccessException e) {
				throw new HibernateException( "Unexpected error trying to determine REF_CURSOR field value : " + e.getMessage() );
			}
		}
		return refCursorTypeCode;
	}


	private static Method getResultSetByPositionMethod;

	private Method getResultSetByPositionMethod() {
		if ( getResultSetByPositionMethod == null ) {
			try {
				getResultSetByPositionMethod = CallableStatement.class.getMethod( "getObject", int.class, Class.class );
			}
			catch (NoSuchMethodException e) {
				throw new HibernateException( "CallableStatement class does not define getObject(int,Class) method" );
			}
			catch (Exception e) {
				throw new HibernateException( "Unexpected error trying to access CallableStatement#getObject(int,Class)" );
			}
		}
		return getResultSetByPositionMethod;
	}


	private static Method getResultSetByNameMethod;

	private Method getResultSetByNameMethod() {
		if ( getResultSetByNameMethod == null ) {
			try {
				getResultSetByNameMethod = CallableStatement.class.getMethod( "getObject", String.class, Class.class );
			}
			catch (NoSuchMethodException e) {
				throw new HibernateException( "CallableStatement class does not define getObject(String,Class) method" );
			}
			catch (Exception e) {
				throw new HibernateException( "Unexpected error trying to access CallableStatement#getObject(String,Class)" );
			}
		}
		return getResultSetByNameMethod;
	}
}
