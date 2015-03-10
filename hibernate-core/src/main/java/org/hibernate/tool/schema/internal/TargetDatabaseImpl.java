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
package org.hibernate.tool.schema.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.Target;

/**
 * @author Steve Ebersole
 */
public class TargetDatabaseImpl implements Target {
	private final JdbcConnectionAccess connectionAccess;

	private Connection connection;
	private Statement statement;

	public TargetDatabaseImpl(JdbcConnectionAccess connectionAccess) {
		this.connectionAccess = connectionAccess;
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return true;
	}

	@Override
	public void prepare() {
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to open JDBC connection for schema management target", e );
		}

		try {
			statement = connection.createStatement();
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to create JDBC Statement for schema management target", e );
		}
	}

	@Override
	public void accept(String action) {
		try {
			statement.executeUpdate( action );
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to execute schema management to JDBC target [" + action + "]", e );
		}
	}

	@Override
	public void release() {
		if ( statement != null ) {
			try {
				statement.close();
			}
			catch (SQLException ignore) {
			}
		}
		if ( connection != null ) {
			try {
				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}
}
