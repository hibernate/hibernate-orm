/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.schema.extract.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;

/**
 * @author Steve Ebersole
 */
public class ExtractionContextImpl implements ExtractionContext {
	private final JdbcEnvironment jdbcEnvironment;
	private final JdbcConnectionAccess jdbcConnectionAccess;
	private final RegisteredObjectAccess registeredTableAccess;

	private Connection jdbcConnection;
	private DatabaseMetaData jdbcDatabaseMetaData;

	public ExtractionContextImpl(
			JdbcEnvironment jdbcEnvironment,
			JdbcConnectionAccess jdbcConnectionAccess,
			RegisteredObjectAccess registeredTableAccess) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.jdbcConnectionAccess = jdbcConnectionAccess;
		this.registeredTableAccess = registeredTableAccess;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public Connection getJdbcConnection() {
		if ( jdbcConnection == null ) {
			try {
				jdbcConnection = jdbcConnectionAccess.obtainConnection();
			}
			catch (SQLException e) {
				throw jdbcEnvironment.getSqlExceptionHelper().convert( e, "Unable to obtain JDBC Connection" );
			}
		}
		return jdbcConnection;
	}

	@Override
	public DatabaseMetaData getJdbcDatabaseMetaData() {
		if ( jdbcDatabaseMetaData == null ) {
			try {
				jdbcDatabaseMetaData = getJdbcConnection().getMetaData();
			}
			catch (SQLException e) {
				throw jdbcEnvironment.getSqlExceptionHelper().convert( e, "Unable to obtain JDBC DatabaseMetaData" );
			}
		}
		return jdbcDatabaseMetaData;
	}

	@Override
	public RegisteredObjectAccess getRegisteredObjectAccess() {
		return registeredTableAccess;
	}

	public void cleanup() {
		if ( jdbcDatabaseMetaData != null ) {
			jdbcDatabaseMetaData = null;
		}

		if ( jdbcConnection != null ) {
			try {
				jdbcConnectionAccess.releaseConnection( jdbcConnection );
			}
			catch (SQLException ignore) {
			}
		}
	}
}
