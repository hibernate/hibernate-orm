/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;

import org.jboss.logging.Logger;

/**
 * Default implementation
 *
 * @author Steve Ebersole
 */
public class DefaultSchemaNameResolver implements SchemaNameResolver {
	private static final Logger log = Logger.getLogger( DefaultSchemaNameResolver.class );

	public static final DefaultSchemaNameResolver INSTANCE = new DefaultSchemaNameResolver();

	private SchemaNameResolver delegate;

	private DefaultSchemaNameResolver() {
	}

	private void determineAppropriateResolverDelegate(Connection connection) {
		// unfortunately Connection#getSchema is only available in Java 1.7 and above
		// and Hibernate still baselines on 1.6.  So for now, use reflection and
		// leverage the Connection#getSchema method if it is available.
		try {
			final Class<? extends Connection> jdbcConnectionClass = connection.getClass();
			final Method getSchemaMethod = jdbcConnectionClass.getMethod( "getSchema" );
			if ( getSchemaMethod != null && getSchemaMethod.getReturnType().equals( String.class ) ) {
				try {
					// If the JDBC driver does not implement the Java 7 spec, but the JRE is Java 7
					// then the getSchemaMethod is not null but the call to getSchema() throws an java.lang.AbstractMethodError
					delegate = new SchemaNameResolverJava17Delegate( getSchemaMethod );
					// Connection#getSchema was introduced into jdk7.
					// Since 5.1 is supposed to have jdk6 source, we can't call Connection#getSchema directly.
					// Make sure it's possible to resolve the schema without taking dialect into account.
					delegate.resolveSchemaName( connection, null );
				}
				catch (java.lang.AbstractMethodError e) {
					log.debugf( "Unable to use Java 1.7 Connection#getSchema" );
					delegate = SchemaNameResolverFallbackDelegate.INSTANCE;
				}
			}
			else {
				log.debugf( "Unable to use Java 1.7 Connection#getSchema" );
				delegate = SchemaNameResolverFallbackDelegate.INSTANCE;
			}
		}
		catch (Exception ignore) {
			delegate = SchemaNameResolverFallbackDelegate.INSTANCE;
			log.debugf( "Unable to resolve connection default schema : " + ignore.getMessage() );
		}
	}

	@Override
	public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
		determineAppropriateResolverDelegate( connection );
		return delegate.resolveSchemaName( connection, dialect );
	}

	public static class SchemaNameResolverJava17Delegate implements SchemaNameResolver {
		private final Method getSchemaMethod;

		public SchemaNameResolverJava17Delegate(Method getSchemaMethod) {
			this.getSchemaMethod = getSchemaMethod;
		}

		@Override
		public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
			try {
				return (String) getSchemaMethod.invoke( connection );
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to invoke Connection#getSchema method via reflection", e );
			}
		}
	}

	public static class SchemaNameResolverFallbackDelegate implements SchemaNameResolver {
		/**
		 * Singleton access
		 */
		public static final SchemaNameResolverFallbackDelegate INSTANCE = new SchemaNameResolverFallbackDelegate();

		@Override
		public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
			final String command = dialect.getCurrentSchemaCommand();
			if ( command == null ) {
				throw new HibernateException(
						"Use of DefaultSchemaNameResolver requires Dialect to provide the " +
								"proper SQL statement/command but provided Dialect [" +
								dialect.getClass().getName() + "] did not return anything " +
								"from Dialect#getCurrentSchemaCommand"
				);
			}

			final Statement statement = connection.createStatement();
			try {
				final ResultSet resultSet = statement.executeQuery( dialect.getCurrentSchemaCommand() );
				try {
					return resultSet.next() ? resultSet.getString( 1 ) : null;
				}
				finally {
					try {
						resultSet.close();
					}
					catch (SQLException ignore) {
					}
				}
			}
			finally {
				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
		}
	}
}
