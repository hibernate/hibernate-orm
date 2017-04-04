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

	private final SchemaNameResolver delegate;

	public DefaultSchemaNameResolver() {
		this.delegate = determineAppropriateResolverDelegate();
	}

	private static SchemaNameResolver determineAppropriateResolverDelegate() {
		// unfortunately Connection#getSchema is only available in Java 1.7 and above
		// and Hibernate still baselines on 1.6.  So for now, use reflection and
		// leverage the Connection#getSchema method if it is available.
		final Class<Connection> jdbcConnectionClass = Connection.class;
		try {
			final Method getSchemaMethod = jdbcConnectionClass.getMethod( "getSchema" );
			if ( getSchemaMethod != null ) {
				if ( getSchemaMethod.getReturnType().equals( String.class ) ) {
					return new SchemaNameResolverJava17Delegate( getSchemaMethod );
				}
			}
		}
		catch (Exception ignore) {
		}

		log.debugf( "Unable to use Java 1.7 Connection#getSchema" );
		return SchemaNameResolverFallbackDelegate.INSTANCE;
	}

	@Override
	public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
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
					if ( !resultSet.next() ) {
						return null;
					}
					return resultSet.getString( 1 );
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
