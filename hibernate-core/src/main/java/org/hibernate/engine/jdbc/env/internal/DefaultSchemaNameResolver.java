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

	// NOTE: The actual delegate should not be cached in DefaultSchemaNameResolver because,
	//       in the case of multiple data sources, there may be a data source that
	//       requires a different delegate. See HHH-12392.

	private DefaultSchemaNameResolver() {
	}

	private SchemaNameResolver determineAppropriateResolverDelegate(Connection connection) {
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
					connection.getSchema();
					return new SchemaNameResolverJava17Delegate();
				}
				catch (java.lang.AbstractMethodError e) {
					log.debugf( "Unable to use Java 1.7 Connection#getSchema" );
					return SchemaNameResolverFallbackDelegate.INSTANCE;
				}
			}
			else {
				log.debugf( "Unable to use Java 1.7 Connection#getSchema" );
				return SchemaNameResolverFallbackDelegate.INSTANCE;
			}
		}
		catch (Exception ignore) {
			log.debugf(
					"Unable to use Java 1.7 Connection#getSchema : An error occurred trying to resolve the connection default schema resolver: "
							+ ignore.getMessage() );
			return SchemaNameResolverFallbackDelegate.INSTANCE;
		}
	}

	@Override
	public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
		// NOTE: delegate should not be cached in DefaultSchemaNameResolver because,
		//       in the case of multiple data sources, there may be a data source that
		//       requires a different delegate. See HHH-12392.
		final SchemaNameResolver delegate = determineAppropriateResolverDelegate( connection );
		return delegate.resolveSchemaName( connection, dialect );
	}

	public static class SchemaNameResolverJava17Delegate implements SchemaNameResolver {

		@Override
		public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
			return connection.getSchema();
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

			try (
				final Statement statement = connection.createStatement();
				final ResultSet resultSet = statement.executeQuery( dialect.getCurrentSchemaCommand() )
			) {
				return resultSet.next() ? resultSet.getString( 1 ) : null;
			}
		}
	}
}
