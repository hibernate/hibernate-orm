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

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.util.JTAHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.transaction.CacheSynchronization;
import org.hibernate.transaction.TransactionFactory;

/**
 * Acts as the mediary between "entity-mode related" sessions in terms of
 * their interaction with the JDBC data store.
 *
 * @author Steve Ebersole
 */
public class JDBCContext implements Serializable, ConnectionManager.Callback {

	// TODO : make this the factory for "entity mode related" sessions;
	// also means making this the target of transaction-synch and the
	// thing that knows how to cascade things between related sessions
	//
	// At that point, perhaps this thing is a "SessionContext", and
	// ConnectionManager is a "JDBCContext"?  A "SessionContext" should
	// live in the impl package...

	private static final Logger log = LoggerFactory.getLogger( JDBCContext.class );

	public static interface Context extends TransactionFactory.Context {
		/**
		 * We cannot rely upon this method being called! It is only
		 * called if we are using Hibernate Transaction API.
		 */
		public void afterTransactionBegin(Transaction tx);
		public void beforeTransactionCompletion(Transaction tx);
		public void afterTransactionCompletion(boolean success, Transaction tx);
		public ConnectionReleaseMode getConnectionReleaseMode();
		public boolean isAutoCloseSessionEnabled();
	}

	private Context owner;
	private ConnectionManager connectionManager;
	private transient boolean isTransactionCallbackRegistered;
	private transient Transaction hibernateTransaction;

	public JDBCContext(Context owner, Connection connection, Interceptor interceptor) {
		this.owner = owner;
		this.connectionManager = new ConnectionManager(
		        owner.getFactory(),
		        this,
		        owner.getConnectionReleaseMode(),
		        connection,
		        interceptor
			);

		final boolean registerSynchronization = owner.isAutoCloseSessionEnabled()
		        || owner.isFlushBeforeCompletionEnabled()
		        || owner.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION;
		if ( registerSynchronization ) {
			registerSynchronizationIfPossible();
		}
	}

	/**
	 * Private constructor used exclusively for custom serialization...
	 *
	 */
	private JDBCContext() {
	}

	// ConnectionManager.Callback implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void connectionOpened() {
		if ( owner.getFactory().getStatistics().isStatisticsEnabled() ) {
			owner.getFactory().getStatisticsImplementor().connect();
		}
	}

	public void connectionCleanedUp() {
		if ( !isTransactionCallbackRegistered ) {
			afterTransactionCompletion( false, null );
			// Note : success = false, because we don't know the outcome of the transaction
		}
	}

	public SessionFactoryImplementor getFactory() {
		return owner.getFactory();
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public Connection borrowConnection() {
		return connectionManager.borrowConnection();
	}
	
	public Connection connection() throws HibernateException {
		if ( owner.isClosed() ) {
			throw new SessionException( "Session is closed" );
		}

		return connectionManager.getConnection();
	}

	public boolean registerCallbackIfNecessary() {
		if ( isTransactionCallbackRegistered ) {
			return false;
		}
		else {
			isTransactionCallbackRegistered = true;
			return true;
		}

	}

	public boolean registerSynchronizationIfPossible() {
		if ( isTransactionCallbackRegistered ) {
			// we already have a callback registered; either a local
			// (org.hibernate.Transaction) transaction has accepted
			// callback responsibilities, or we have previously
			// registered a transaction synch.
			return true;
		}
		boolean localCallbacksOnly = owner.getFactory().getSettings()
				.getTransactionFactory()
				.areCallbacksLocalToHibernateTransactions();
		if ( localCallbacksOnly ) {
			// the configured transaction-factory says it only supports
			// local callback mode, so no sense attempting to register a
			// JTA Synchronization
			return false;
		}
		TransactionManager tm = owner.getFactory().getTransactionManager();
		if ( tm == null ) {
			// if there is no TM configured, we will not be able to access
			// the javax.transaction.Transaction object in order to
			// register a synch anyway.
			return false;
		}
		else {
			try {
				if ( !isTransactionInProgress() ) {
					log.trace( "TransactionFactory reported no active transaction; Synchronization not registered" );
					return false;
				}
				else {
					javax.transaction.Transaction tx = tm.getTransaction();
					if ( JTAHelper.isMarkedForRollback( tx ) ) {
						// transactions marked for rollback-only cause some TM impls to throw exceptions
						log.debug( "Transaction is marked for rollback; skipping Synchronization registration" );
						return false;
					}
					else {
						if ( hibernateTransaction == null ) {
							hibernateTransaction = owner.getFactory().getSettings().getTransactionFactory().createTransaction( this, owner );
						}
						tx.registerSynchronization( new CacheSynchronization(owner, this, tx, hibernateTransaction) );
						isTransactionCallbackRegistered = true;
						log.debug("successfully registered Synchronization");
						return true;
					}
				}
			}
			catch( HibernateException e ) {
				throw e;
			}
			catch (Exception e) {
				throw new TransactionException( "could not register synchronization with JTA TransactionManager", e );
			}
		}
	}
	
	public boolean isTransactionInProgress() {
		return owner.getFactory().getSettings().getTransactionFactory()
				.isTransactionInProgress( this, owner, hibernateTransaction );
	}

	public Transaction getTransaction() throws HibernateException {
		if (hibernateTransaction==null) {
			hibernateTransaction = owner.getFactory().getSettings()
					.getTransactionFactory()
					.createTransaction( this, owner );
		}
		return hibernateTransaction;
	}
	
	public void beforeTransactionCompletion(Transaction tx) {
		log.trace( "before transaction completion" );
		owner.beforeTransactionCompletion(tx);
	}
	
	/**
	 * We cannot rely upon this method being called! It is only
	 * called if we are using Hibernate Transaction API.
	 */
	public void afterTransactionBegin(Transaction tx) {
		log.trace( "after transaction begin" );
		owner.afterTransactionBegin(tx);
	}

	public void afterTransactionCompletion(boolean success, Transaction tx) {
		log.trace( "after transaction completion" );

		if ( getFactory().getStatistics().isStatisticsEnabled() ) {
			getFactory().getStatisticsImplementor().endTransaction(success);
		}

		connectionManager.afterTransaction();

		isTransactionCallbackRegistered = false;
		hibernateTransaction = null;
		owner.afterTransactionCompletion(success, tx);
	}
	
	/**
	 * Called after executing a query outside the scope of
	 * a Hibernate or JTA transaction
	 */
	public void afterNontransactionalQuery(boolean success) {
		log.trace( "after autocommit" );
		try {
			// check to see if the connection is in auto-commit 
			// mode (no connection means aggressive connection
			// release outside a JTA transaction context, so MUST
			// be autocommit mode)
			boolean isAutocommit = connectionManager.isAutoCommit();

			connectionManager.afterTransaction();
			
			if ( isAutocommit ) {
				owner.afterTransactionCompletion(success, null);
			}
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert( 
					owner.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not inspect JDBC autocommit mode"
				);
		}
	}


	// serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void writeObject(ObjectOutputStream oos) throws IOException {
		// isTransactionCallbackRegistered denotes whether any Hibernate
		// Transaction has registered as a callback against this
		// JDBCContext; only one such callback is allowed.  Directly
		// serializing this value causes problems with JDBCTransaction,
		// or really any Transaction impl where the callback is local
		// to the Transaction instance itself, since that Transaction
		// is not serialized along with the JDBCContext.  Thus we
		// handle that fact here explicitly...
		oos.defaultWriteObject();
		boolean deserHasCallbackRegistered = isTransactionCallbackRegistered
				&& ! owner.getFactory().getSettings().getTransactionFactory()
				.areCallbacksLocalToHibernateTransactions();
		oos.writeBoolean( deserHasCallbackRegistered );
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		isTransactionCallbackRegistered = ois.readBoolean();
	}

	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		connectionManager.serialize( oos );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @throws IOException
	 */
	public static JDBCContext deserialize(
			ObjectInputStream ois,
	        Context context,
	        Interceptor interceptor) throws IOException {
		JDBCContext jdbcContext = new JDBCContext();
		jdbcContext.owner = context;
		jdbcContext.connectionManager = ConnectionManager.deserialize(
				ois,
				context.getFactory(),
		        interceptor,
		        context.getConnectionReleaseMode(),
		        jdbcContext
		);
		return jdbcContext;
	}
}
