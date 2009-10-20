/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.resolver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.dialect.resolver.DialectResolver;
import org.hibernate.dialect.resolver.DialectResolverSet;
import org.hibernate.dialect.resolver.StandardDialectResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for generating Dialect instances.
 *
 * @author Steve Ebersole
 * @author Tomoto Shimizu Washio
 */
public class DialectFactory {
	private static final Logger log = LoggerFactory.getLogger( DialectFactory.class );

	private static DialectResolverSet DIALECT_RESOLVERS = new DialectResolverSet();

	static {
		// register the standard dialect resolver
		DIALECT_RESOLVERS.addResolver( new StandardDialectResolver() );

		// register resolvers set via Environment property
		String userSpecifedResolverSetting = Environment.getProperties().getProperty( Environment.DIALECT_RESOLVERS );
		if ( userSpecifedResolverSetting != null ) {
			String[] userSpecifedResolvers = userSpecifedResolverSetting.split( "\\s+" );
			for ( int i = 0; i < userSpecifedResolvers.length; i++ ) {
				registerDialectResolver( userSpecifedResolvers[i] );
			}
		}
	}

	/*package*/ static void registerDialectResolver(String resolverName) {
		try {
			DialectResolver resolver = ( DialectResolver ) ReflectHelper.classForName( resolverName ).newInstance();
			DIALECT_RESOLVERS.addResolverAtFirst( resolver );
		}
		catch ( ClassNotFoundException cnfe ) {
			log.warn( "Dialect resolver class not found: " + resolverName );
		}
		catch ( Exception e ) {
			log.warn( "Could not instantiate dialect resolver class", e );
		}
	}

	/**
	 * Builds an appropriate Dialect instance.
	 * <p/>
	 * If a dialect is explicitly named in the incoming properties, it is used. Otherwise, it is
	 * determined by dialect resolvers based on the passed connection.
	 * <p/>
	 * An exception is thrown if a dialect was not explicitly set and no resolver could make
	 * the determination from the given connection.
	 *
	 * @param properties The configuration properties.
	 * @param connection The configured connection.
	 * @return The appropriate dialect instance.
	 * @throws HibernateException No dialect specified and no resolver could make the determination.
	 */
	public static Dialect buildDialect(Properties properties, Connection connection) throws HibernateException {
		String dialectName = properties.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			return determineDialect( connection );
		}
		else {
			return constructDialect( dialectName );
		}
	}

	public static  Dialect buildDialect(Properties properties) {
		String dialectName = properties.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			throw new HibernateException( "'hibernate.dialect' must be set when no Connection available" );
		}
		return constructDialect( dialectName );
	}

	/**
	 * Determine the appropriate Dialect to use given the connection.
	 *
	 * @param connection The configured connection.
	 * @return The appropriate dialect instance.
	 *
	 * @throws HibernateException No connection given or no resolver could make
	 * the determination from the given connection.
	 */
	private static Dialect determineDialect(Connection connection) {
		if ( connection == null ) {
			throw new HibernateException( "Connection cannot be null when 'hibernate.dialect' not set" );
		}

		try {
			final DatabaseMetaData databaseMetaData = connection.getMetaData();
			final Dialect dialect = DIALECT_RESOLVERS.resolveDialect( databaseMetaData );

			if ( dialect == null ) {
				throw new HibernateException(
						"Unable to determine Dialect to use [name=" + databaseMetaData.getDatabaseProductName() +
								", majorVersion=" + databaseMetaData.getDatabaseMajorVersion() +
								"]; user must register resolver or explicitly set 'hibernate.dialect'"
				);
			}

			return dialect;
		}
		catch ( SQLException sqlException ) {
			throw new HibernateException(
					"Unable to access java.sql.DatabaseMetaData to determine appropriate Dialect to use",
					sqlException
			);
		}
	}

	/**
	 * Returns a dialect instance given the name of the class to use.
	 *
	 * @param dialectName The name of the dialect class.
	 *
	 * @return The dialect instance.
	 */
	public static Dialect constructDialect(String dialectName) {
		try {
			return ( Dialect ) ReflectHelper.classForName( dialectName ).newInstance();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new HibernateException( "Dialect class not found: " + dialectName, cnfe );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate dialect class", e );
		}
	}
}
