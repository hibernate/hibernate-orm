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
package org.hibernate.engine.transaction.spi;

import java.io.Serializable;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

/**
 * Access to services needed in the context of processing transaction requests.
 * <p/>
 * The context is roughly speaking equivalent to the Hibernate session, as opposed to the {@link TransactionEnvironment}
 * which is roughly equivalent to the Hibernate session factory
 * 
 * @author Steve Ebersole
 */
public interface TransactionContext extends Serializable {
	/**
	 * Obtain the {@link TransactionEnvironment} associated with this context.
	 *
	 * @return The transaction environment.
	 */
	public TransactionEnvironment getTransactionEnvironment();

	/**
	 * Is this context already closed?
	 *
	 * @return {@literal true}/{@literal false} appropriately.
	 */
	public boolean isClosed();

	/**
	 * Should flushes only happen manually for this context?
	 *
	 * @return {@literal true}/{@literal false} appropriately.
	 */
	public boolean isFlushModeNever();

	/**
	 * Perform a managed flush.
	 */
	public void managedFlush();

	/**
	 * Perform a managed close.
	 */
	public void managedClose();

	public String onPrepareStatement(String sql);

	public JdbcConnectionAccess getJdbcConnectionAccess();
}
