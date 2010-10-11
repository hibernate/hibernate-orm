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
package org.hibernate.service.jdbc.spi;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Service;

/**
 * Contract for services around JDBC operations.  These represent shared resources, aka not varied by session/use.
 *
 * @author Steve Ebersole
 */
public interface JdbcServices extends Service {
	/**
	 * Obtain service for providing JDBC connections.
	 *
	 * @return The connection provider.
	 */
	public ConnectionProvider getConnectionProvider();

	/**
	 * Obtain the dialect of the database to which {@link Connection connections} from
	 * {@link #getConnectionProvider()} point.
	 *
	 * @return The database dialect.
	 */
	public Dialect getDialect();

	/**
	 * Obtain service for logging SQL statements.
	 *
	 * @return The SQL statement logger.
	 */
	public SQLStatementLogger getSqlStatementLogger();

	/**
	 * Obtain service for dealing with exceptions.
	 *
	 * @return The exception helper service.
	 */
	public SQLExceptionHelper getSqlExceptionHelper();

	/**
	 * Obtain infomration about supported behavior reported by the JDBC driver.
	 * <p/>
	 * Yuck, yuck, yuck!  Much prefer this to be part of a "basic settings" type object.  See discussion
	 * on {@link org.hibernate.cfg.internal.JdbcServicesBuilder}
	 * 
	 * @return
	 */
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport();
}
