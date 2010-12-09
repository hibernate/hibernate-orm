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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;

/**
 * Manages prepared statements and batching.
 *
 * @author Gavin King
 */
public abstract class AbstractBatcher implements Batcher {

	protected static final Logger log = LoggerFactory.getLogger( AbstractBatcher.class );

	private final SQLExceptionHelper exceptionHelper;
	private final int jdbcBatchSize;

	private PreparedStatement batchUpdate;
	private String batchUpdateSQL;
	private boolean isClosingBatchUpdate = false;

	public AbstractBatcher(SQLExceptionHelper exceptionHelper, int jdbcBatchSize) {
		this.exceptionHelper = exceptionHelper;
		this.jdbcBatchSize = jdbcBatchSize;
	}

	public final int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public boolean hasOpenResources() {
		try {
			return !isClosingBatchUpdate && batchUpdate != null && ! batchUpdate.isClosed();
		}
		catch (SQLException sqle) {
			throw exceptionHelper.convert(
					sqle,
					"Could check to see if batch statement was closed",
					batchUpdateSQL
				);
		}
	}

	public PreparedStatement getStatement(String sql) {
		return batchUpdate != null && batchUpdateSQL.equals( sql ) ? batchUpdate : null;
	}

	public void setStatement(String sql, PreparedStatement ps) {
		checkNotClosingBatchUpdate();
		batchUpdateSQL = sql;
		batchUpdate = ps;		
	}

	protected PreparedStatement getStatement() {
		return batchUpdate;
	}

	public void abortBatch(SQLException sqle) {
		closeStatements();
	}

	/**
	 * Actually releases the batcher, allowing it to cleanup internally held
	 * resources.
	 */
	public void closeStatements() {
		try {
			closeBatchUpdate();
		}
		catch ( SQLException sqle ) {
			//no big deal
			log.warn( "Could not close a JDBC prepared statement", sqle );
		}
		batchUpdate = null;
		batchUpdateSQL = null;
	}

	public void executeBatch() throws HibernateException {
		checkNotClosingBatchUpdate();
		if (batchUpdate!=null) {
			try {
				try {
					doExecuteBatch(batchUpdate);
				}
				finally {
					closeBatchUpdate();
				}
			}
			catch (SQLException sqle) {
				throw exceptionHelper.convert(
				        sqle,
				        "Could not execute JDBC batch update",
				        batchUpdateSQL
					);
			}
			finally {
				batchUpdate=null;
				batchUpdateSQL=null;
			}
		}
	}

	protected abstract void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException;


	private void closeBatchUpdate() throws SQLException{
		checkNotClosingBatchUpdate();
		try {
			if ( batchUpdate != null ) {
				isClosingBatchUpdate = true;
				batchUpdate.close();
			}
		}
		finally {
			isClosingBatchUpdate = false;
		}

	}

	private void checkNotClosingBatchUpdate() {
		if ( isClosingBatchUpdate ) {
			throw new IllegalStateException( "Cannot perform operation while closing batch update." );
		}
	}
}






