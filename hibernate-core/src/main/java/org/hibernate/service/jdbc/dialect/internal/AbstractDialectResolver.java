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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jboss.logging.Logger;

import org.hibernate.JDBCException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.resolver.BasicSQLExceptionConverter;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;

/**
 * A templated resolver impl which delegates to the {@link #resolveDialectInternal} method
 * and handles any thrown {@link SQLException SQL errors}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDialectResolver implements DialectResolver {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       AbstractDialectResolver.class.getName());

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here we template the resolution, delegating to {@link #resolveDialectInternal} and handling
	 * {@link java.sql.SQLException}s properly.
	 */
	public final Dialect resolveDialect(DatabaseMetaData metaData) {
		try {
			return resolveDialectInternal( metaData );
		}
		catch ( SQLException sqlException ) {
			JDBCException jdbcException = BasicSQLExceptionConverter.INSTANCE.convert( sqlException );
            if (jdbcException instanceof JDBCConnectionException) throw jdbcException;
            LOG.warnf("%s : %s", BasicSQLExceptionConverter.MSG, sqlException.getMessage());
            return null;
		}
		catch ( Throwable t ) {
            LOG.unableToExecuteResolver(this, t.getMessage());
			return null;
		}
	}

	/**
	 * Perform the actual resolution without caring about handling {@link SQLException}s.
	 *
	 * @param metaData The database metadata
	 * @return The resolved dialect, or null if we could not resolve.
	 * @throws SQLException Indicates problems accessing the metadata.
	 */
	protected abstract Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException;
}
