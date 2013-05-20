/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Contract for extracting ResultSets from Statements, executing Statements,
 * managing Statement/ResultSet resources, and logging statement calls.
 * 
 * TODO: This could eventually utilize the new Return interface.  It would be
 * great to have a common API shared.
 *
 * Generally the methods here dealing with CallableStatement are extremely limited, relying on the legacy
 *
 * 
 * @author Brett Meyer
 * @author Steve Ebersole
 */
public interface ResultSetReturn {
	
	/**
	 * Extract the ResultSet from the PreparedStatement.
	 * <p/>
	 * If user passes {@link CallableStatement} reference, this method calls {@link #extract(CallableStatement)}
	 * internally.  Otherwise, generally speaking, {@link java.sql.PreparedStatement#executeQuery()} is called
	 *
	 * @param statement The PreparedStatement from which to extract the ResultSet
	 *
	 * @return The extracted ResultSet
	 */
	public ResultSet extract(PreparedStatement statement);
	
	/**
	 * Extract the ResultSet from the CallableStatement.  Note that this is the limited legacy form which delegates to
	 * {@link org.hibernate.dialect.Dialect#getResultSet}.  Better option is to integrate
	 * {@link org.hibernate.procedure.ProcedureCall}-like hooks
	 *
	 * @param callableStatement The CallableStatement from which to extract the ResultSet
	 *
	 * @return The extracted ResultSet
	 */
	public ResultSet extract(CallableStatement callableStatement);
	
	/**
	 * Performs the given SQL statement, expecting a ResultSet in return
	 *
	 * @param statement The JDBC Statement object to use
	 * @param sql The SQL to execute
	 *
	 * @return The resulting ResultSet
	 */
	public ResultSet extract(Statement statement, String sql);
	
	/**
	 * Execute the PreparedStatement return its first ResultSet, if any.  If there is no ResultSet, returns {@code null}
	 *
	 * @param statement The PreparedStatement to execute
	 *
	 * @return The extracted ResultSet, or {@code null}
	 */
	public ResultSet execute(PreparedStatement statement);
	
	/**
	 * Performs the given SQL statement, returning its first ResultSet, if any.  If there is no ResultSet,
	 * returns {@code null}
	 *
	 * @param statement The JDBC Statement object to use
	 * @param sql The SQL to execute
	 *
	 * @return The extracted ResultSet, or {@code null}
	 */
	public ResultSet execute(Statement statement, String sql);
	
	/**
	 * Execute the PreparedStatement, returning its "affected row count".
	 *
	 * @param statement The PreparedStatement to execute
	 *
	 * @return The {@link java.sql.PreparedStatement#executeUpdate()} result
	 */
	public int executeUpdate(PreparedStatement statement);
	
	/**
	 * Execute the given SQL statement returning its "affected row count".
	 *
	 * @param statement The JDBC Statement object to use
	 * @param sql The SQL to execute
	 *
	 * @return The {@link java.sql.PreparedStatement#executeUpdate(String)} result
	 */
	public int executeUpdate(Statement statement, String sql);
}
