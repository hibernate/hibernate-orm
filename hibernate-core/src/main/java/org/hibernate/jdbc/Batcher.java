/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;

/**
 * Manages <tt>PreparedStatement</tt>s for a session. Abstracts JDBC
 * batching to maintain the illusion that a single logical batch
 * exists for the whole session, even when batching is disabled.
 * Provides transparent <tt>PreparedStatement</tt> caching.
 *
 * @see java.sql.PreparedStatement
 * @see org.hibernate.impl.SessionImpl
 * @author Gavin King
 */
public interface Batcher {

	public PreparedStatement getStatement(String sql);
	public void setStatement(String sql, PreparedStatement ps);
	public boolean hasOpenResources();

	/**
	 * Add an insert / delete / update to the current batch (might be called multiple times
	 * for single <tt>prepareBatchStatement()</tt>)
	 */
	public void addToBatch(Expectation expectation) throws SQLException, HibernateException;

	/**
	 * Execute the batch
	 */
	public void executeBatch() throws HibernateException;

	/**
	 * Must be called when an exception occurs
	 * @param sqle the (not null) exception that is the reason for aborting
	 */
	public void abortBatch(SQLException sqle);

	/**
	 * Actually releases the batcher, allowing it to cleanup internally held
	 * resources.
	 */
	public void closeStatements();	
}

