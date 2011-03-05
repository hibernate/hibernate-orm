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

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;

import java.io.Serializable;
import java.sql.Connection;

/**
 * Coordinates JDBC-related activities.
 *
 * @author Steve Ebersole
 */
public interface JdbcCoordinator extends Serializable {
	/**
	 * Retrieve the transaction coordinator associated with this JDBC coordinator.
	 *
	 * @return The transaction coordinator
	 */
	public TransactionCoordinator getTransactionCoordinator();

	/**
	 * Retrieves the logical connection associated with this JDBC coordinator.
	 *
	 * @return The logical connection
	 */
	public LogicalConnectionImplementor getLogicalConnection();

	/**
	 * Get a batch instance.
	 *
	 * @param key The unique batch key.
	 *
	 * @return The batch
	 */
	public Batch getBatch(BatchKey key);

	public void abortBatch();

	/**
	 * Obtain the statement preparer associated with this JDBC coordinator.
	 *
	 * @return This coordinator's statement preparer
	 */
	public StatementPreparer getStatementPreparer();

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	public void flushBeginning();

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	public void flushEnding();

	public Connection close();

	public void afterTransaction();

	public void coordinateWork(Work work);

	public <T> T coordinateWork(ReturningWork<T> work);

	public void executeBatch();

	public void cancelLastQuery();

	public void setTransactionTimeOut(int timeout);

}
