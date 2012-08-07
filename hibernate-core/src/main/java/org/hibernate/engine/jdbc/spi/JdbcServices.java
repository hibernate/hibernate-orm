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
package org.hibernate.engine.jdbc.spi;

import java.sql.ResultSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.service.Service;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

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
	 *
	 * @deprecated See deprecation notice on {@link org.hibernate.engine.spi.SessionFactoryImplementor#getConnectionProvider()}
	 * for details
	 */
	@Deprecated
	public ConnectionProvider getConnectionProvider();

	/**
	 * Obtain the dialect of the database.
	 *
	 * @return The database dialect.
	 */
	public Dialect getDialect();

	/**
	 * Obtain service for logging SQL statements.
	 *
	 * @return The SQL statement logger.
	 */
	public SqlStatementLogger getSqlStatementLogger();

	/**
	 * Obtain service for dealing with exceptions.
	 *
	 * @return The exception helper service.
	 */
	public SqlExceptionHelper getSqlExceptionHelper();

	/**
	 * Obtain information about supported behavior reported by the JDBC driver.
	 * <p/>
	 * Yuck, yuck, yuck!  Much prefer this to be part of a "basic settings" type object.
	 * 
	 * @return The extracted database metadata, oddly enough :)
	 */
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport();

	/**
	 * Create an instance of a {@link LobCreator} appropriate for the current environment, mainly meant to account for
	 * variance between JDBC 4 (<= JDK 1.6) and JDBC3 (>= JDK 1.5).
	 *
	 * @param lobCreationContext The context in which the LOB is being created
	 * @return The LOB creator.
	 */
	public LobCreator getLobCreator(LobCreationContext lobCreationContext);

	/**
	 * Obtain service for wrapping a {@link ResultSet} in a "column name cache" wrapper.
	 * @return The ResultSet wrapper.
	 */
	public ResultSetWrapper getResultSetWrapper();
}
