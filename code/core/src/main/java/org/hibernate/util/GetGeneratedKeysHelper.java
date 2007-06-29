//$Id: GetGeneratedKeysHelper.java 9676 2006-03-22 17:38:55Z steve.ebersole@jboss.com $
package org.hibernate.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.AssertionFailure;

/**
 * @author Gavin King
 */
public final class GetGeneratedKeysHelper {

	private GetGeneratedKeysHelper() {
	}

	private static final Integer RETURN_GENERATED_KEYS;
	private static final Method PREPARE_STATEMENT_METHOD;
	private static final Method GET_GENERATED_KEYS_METHOD;

	static {
		try {
			int returnGeneratedKeysEnumValue = Statement.class
					.getDeclaredField( "RETURN_GENERATED_KEYS" )
					.getInt( PreparedStatement.class );
			RETURN_GENERATED_KEYS = new Integer( returnGeneratedKeysEnumValue );
			PREPARE_STATEMENT_METHOD = Connection.class.getMethod(
					"prepareStatement",
			        new Class[] { String.class, Integer.TYPE }
			);
			GET_GENERATED_KEYS_METHOD = Statement.class.getDeclaredMethod(
					"getGeneratedKeys",
			        null
			);
		}
		catch ( Exception e ) {
			throw new AssertionFailure( "could not initialize getGeneratedKeys() support", e );
		}
	}

	public static PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
		Object[] args = new Object[] { sql, RETURN_GENERATED_KEYS } ;
		try {
			return ( PreparedStatement ) PREPARE_STATEMENT_METHOD.invoke( conn, args );
		}
		catch ( InvocationTargetException ite ) {
			if ( ite.getTargetException() instanceof SQLException ) {
				throw ( SQLException ) ite.getTargetException();
			}
			else if ( ite.getTargetException() instanceof RuntimeException ) {
				throw ( RuntimeException ) ite.getTargetException();
			}
			else {
				throw new AssertionFailure( "InvocationTargetException preparing statement capable of returning generated keys (JDBC3)", ite );
			}
		}
		catch ( IllegalAccessException iae ) {
			throw new AssertionFailure( "IllegalAccessException preparing statement capable of returning generated keys (JDBC3)", iae );
		}
	}

	public static ResultSet getGeneratedKey(PreparedStatement ps) throws SQLException {
		try {
			return ( ResultSet ) GET_GENERATED_KEYS_METHOD.invoke( ps, null );
		}
		catch ( InvocationTargetException ite ) {
			if ( ite.getTargetException() instanceof SQLException ) {
				throw ( SQLException ) ite.getTargetException();
			}
			else if ( ite.getTargetException() instanceof RuntimeException ) {
				throw ( RuntimeException ) ite.getTargetException();
			}
			else {
				throw new AssertionFailure( "InvocationTargetException extracting generated keys (JDBC3)", ite );
			}
		}
		catch ( IllegalAccessException iae ) {
			throw new AssertionFailure( "IllegalAccessException extracting generated keys (JDBC3)", iae );
		}
	}

}
