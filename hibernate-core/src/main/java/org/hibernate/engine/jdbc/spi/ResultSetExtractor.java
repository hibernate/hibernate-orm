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
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Contracting for extracting ResultSets from Statements
 * 
 * @author Brett Meyer
 */
public interface ResultSetExtractor {
	
	/**
	 * Extract the ResultSet from the statement.
	 *
	 * @param statement
	 *
	 * @return the ResultSet
	 */
	public ResultSet extract( PreparedStatement statement ) throws SQLException;
	
	/**
	 * Extract the ResultSet from the statement.
	 *
	 * @param statement
	 *
	 * @return the ResultSet
	 */
	public ResultSet extract( CallableStatement statement ) throws SQLException;
	
	/**
	 * Extract the ResultSet from the statement.
	 *
	 * @param statement
	 * @param sql
	 *
	 * @return the ResultSet
	 */
	public ResultSet extract( Statement statement, String sql ) throws SQLException;
	
	/**
	 * Execute the Statement query and, if results in a ResultSet, extract it.
	 *
	 * @param statement
	 *
	 * @return the ResultSet
	 */
	public ResultSet execute( PreparedStatement statement ) throws SQLException;
	
	/**
	 * Execute the Statement query and, if results in a ResultSet, extract it.
	 *
	 * @param statement
	 * @param sql
	 *
	 * @return the ResultSet
	 */
	public ResultSet execute( Statement statement, String sql ) throws SQLException;
	
	/**
	 * Execute the Statement queryUpdate.
	 *
	 * @param statement
	 *
	 * @return int
	 */
	public int executeUpdate( PreparedStatement statement ) throws SQLException;
	
	/**
	 * Execute the Statement query and, if results in a ResultSet, extract it.
	 *
	 * @param statement
	 * @param sql
	 *
	 * @return the ResultSet
	 */
	public int executeUpdate( Statement statement, String sql ) throws SQLException;
}
