/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
