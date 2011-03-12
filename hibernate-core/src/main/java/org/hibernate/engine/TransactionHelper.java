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
package org.hibernate.engine;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.IsolatedWork;
import org.hibernate.engine.transaction.Isolater;
import org.hibernate.exception.JDBCExceptionHelper;

/**
 * Allows work to be done outside the current transaction, by suspending it,
 * and performing work in a new transaction
 * 
 * @author Emmanuel Bernard
 */
public abstract class TransactionHelper {

	// todo : remove this and just have subclasses use Isolater/IsolatedWork directly...

	/**
	 * The work to be done
	 */
	protected abstract Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException;

	/**
	 * Suspend the current transaction and perform work in a new transaction
	 */
	public Serializable doWorkInNewTransaction(final SessionImplementor session)
	throws HibernateException {
		class Work implements IsolatedWork {
			Serializable generatedValue;
			public void doWork(Connection connection) throws HibernateException {
				String sql = null;
				try {
					generatedValue = doWorkInCurrentTransaction( connection, sql );
				}
				catch( SQLException sqle ) {
					throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							sqle,
							"could not get or update next value",
							sql
						);
				}
			}
		}
		Work work = new Work();
		Isolater.doIsolatedWork( work, session );
		return work.generatedValue;
	}
}
