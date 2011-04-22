/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jdbc.dialect.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.service.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;
import org.hibernate.service.spi.InjectService;

/**
 * Standard implementation of the {@link DialectFactory} service.
 *
 * @author Steve Ebersole
 */
public class DialectFactoryImpl implements DialectFactory {
	private ClassLoaderService classLoaderService;

	@InjectService
	public void setClassLoaderService(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	private DialectResolver dialectResolver;

	@InjectService
	public void setDialectResolver(DialectResolver dialectResolver) {
		this.dialectResolver = dialectResolver;
	}

	@Override
	public Dialect buildDialect(Map configValues, Connection connection) throws HibernateException {
		final String dialectName = (String) configValues.get( AvailableSettings.DIALECT );
		if ( dialectName != null ) {
			return constructDialect( dialectName );
		}
		else {
			return determineDialect( connection );
		}
	}

	private Dialect constructDialect(String dialectName) {
		try {
			return ( Dialect ) classLoaderService.classForName( dialectName ).newInstance();
		}
		catch ( ClassLoadingException e ) {
			throw new HibernateException( "Dialect class not found: " + dialectName, e );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate dialect class", e );
		}
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
	private Dialect determineDialect(Connection connection) {
		if ( connection == null ) {
			throw new HibernateException( "Connection cannot be null when 'hibernate.dialect' not set" );
		}

		try {
			final DatabaseMetaData databaseMetaData = connection.getMetaData();
			final Dialect dialect = dialectResolver.resolveDialect( databaseMetaData );

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
}
