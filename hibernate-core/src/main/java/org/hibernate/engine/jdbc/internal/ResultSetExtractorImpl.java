/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetExtractor;

/**
 * @author Brett Meyer
 */
public class ResultSetExtractorImpl implements ResultSetExtractor {

	private final JdbcCoordinator jdbcCoordinator;

	public ResultSetExtractorImpl(JdbcCoordinator jdbcCoordinator) {
		this.jdbcCoordinator = jdbcCoordinator;
	}

	@Override
	public ResultSet extract(PreparedStatement statement) throws SQLException {
		// sql logged by StatementPreparerImpl
		ResultSet rs = statement.executeQuery();
		postExtract( rs );
		return rs;
	}

	@Override
	public ResultSet extract(CallableStatement statement) throws SQLException {
		// sql logged by StatementPreparerImpl
		ResultSet rs = jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getDialect().getResultSet( statement );
		postExtract( rs );
		return rs;
	}

	@Override
	public ResultSet extract(Statement statement, String sql) throws SQLException {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		ResultSet rs = statement.executeQuery( sql );
		postExtract( rs );
		return rs;
	}

	@Override
	public ResultSet execute(PreparedStatement statement) throws SQLException {
		// sql logged by StatementPreparerImpl
		if ( !statement.execute() ) {
			while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
				// do nothing until we hit the resultset
			}
		}
		ResultSet rs = statement.getResultSet();
		postExtract( rs );
		return rs;
	}

	@Override
	public ResultSet execute(Statement statement, String sql) throws SQLException {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		// true if statement has results
		if ( statement.execute( sql ) ) {
			ResultSet rs = statement.getResultSet();
			postExtract( rs );
			return rs;
		}
		// If didn't result in ResultSet, the caller shouldn't care anyway.
		return null;
	}
	
	@Override
	public int executeUpdate( PreparedStatement statement ) throws SQLException {
		// Technically, this method isn't needed at the moment.  But keep and use
		// it for consistency and future usage.
		return statement.executeUpdate();
	}
	
	@Override
	public int executeUpdate( Statement statement, String sql ) throws SQLException {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		return statement.executeUpdate( sql );
	}

	private void postExtract(ResultSet rs) {
		jdbcCoordinator.register( rs );
	}

}
