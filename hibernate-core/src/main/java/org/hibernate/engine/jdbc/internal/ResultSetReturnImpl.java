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
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;

/**
 * @author Brett Meyer
 */
public class ResultSetReturnImpl implements ResultSetReturn {

	private final JdbcCoordinator jdbcCoordinator;

	public ResultSetReturnImpl(JdbcCoordinator jdbcCoordinator) {
		this.jdbcCoordinator = jdbcCoordinator;
	}

	@Override
	public ResultSet extract(PreparedStatement statement) {
		// sql logged by StatementPreparerImpl
		if ( statement instanceof CallableStatement ) {
			// We actually need to extract from Callable statement.  Although
			// this seems needless, Oracle can return an
			// OracleCallableStatementWrapper that finds its way to this method,
			// rather than extract(CallableStatement).  See HHH-8022.
			CallableStatement callableStatement = (CallableStatement) statement;
			return extract( callableStatement );
		}
		try {
			ResultSet rs = statement.executeQuery();
			postExtract( rs, statement );
			return rs;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet extract(CallableStatement statement) {
		try {
			// sql logged by StatementPreparerImpl
			ResultSet rs = jdbcCoordinator.getLogicalConnection().getJdbcServices()
					.getDialect().getResultSet( statement );
			postExtract( rs, statement );
			return rs;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet extract(Statement statement, String sql) {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		try {
			ResultSet rs = statement.executeQuery( sql );
			postExtract( rs, statement );
			return rs;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet execute(PreparedStatement statement) {
		// sql logged by StatementPreparerImpl
		try {
			if ( !statement.execute() ) {
				while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
					// do nothing until we hit the resultset
				}
			}
			ResultSet rs = statement.getResultSet();
			postExtract( rs, statement );
			return rs;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not execute statement" );
		}
	}

	@Override
	public ResultSet execute(Statement statement, String sql) {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		try {
			if ( !statement.execute( sql ) ) {
				while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
					// do nothing until we hit the resultset
				}
			}
			ResultSet rs = statement.getResultSet();
			postExtract( rs, statement );
			return rs;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not execute statement" );
		}
	}
	
	@Override
	public int executeUpdate( PreparedStatement statement ) {
		try {
			return statement.executeUpdate();
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not execute statement" );
		}
	}
	
	@Override
	public int executeUpdate( Statement statement, String sql ) {
		jdbcCoordinator.getLogicalConnection().getJdbcServices()
				.getSqlStatementLogger().logStatement( sql );
		try {
			return statement.executeUpdate( sql );
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "could not execute statement" );
		}
	}

	private final SqlExceptionHelper sqlExceptionHelper() {
		return jdbcCoordinator.getTransactionCoordinator()
				.getTransactionContext()
				.getTransactionEnvironment()
				.getJdbcServices()
				.getSqlExceptionHelper();
	}

	private void postExtract(ResultSet rs, Statement st) {
		if ( rs != null ) jdbcCoordinator.register( rs, st );
	}

}
