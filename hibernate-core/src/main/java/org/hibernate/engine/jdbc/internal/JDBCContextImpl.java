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
package org.hibernate.engine.jdbc.internal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.TransactionManager;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.Interceptor;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.spi.ConnectionManager;
import org.hibernate.engine.jdbc.spi.JDBCContext;
import org.hibernate.transaction.synchronization.CallbackCoordinator;
import org.hibernate.transaction.synchronization.HibernateSynchronizationImpl;
import org.hibernate.util.JTAHelper;
import org.jboss.logging.Logger;

/**
 * Acts as the intermediary between "entity-mode related" sessions in terms of their interaction with the JDBC data store.
 *
 * @author Steve Ebersole
 */
public class JDBCContextImpl implements ConnectionManagerImpl.Callback, JDBCContext {

	// TODO : make this the factory for "entity mode related" sessions;
	// also means making this the target of transaction-synch and the
	// thing that knows how to cascade things between related sessions
	//
	// At that point, perhaps this thing is a "SessionContext", and
	// ConnectionManager is a "JDBCContext"?  A "SessionContext" should
	// live in the impl package...

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, JDBCContextImpl.class.getName());

	private Context owner;
	private ConnectionManagerImpl connectionManager;
	private transient boolean isTransactionCallbackRegistered;
	private transient Transaction hibernateTransaction;

	private CallbackCoordinator jtaSynchronizationCallbackCoordinator;

	public JDBCContextImpl(Context owner, Connection connection, Interceptor interceptor) {
		this.owner = owner;
		this.connectionManager = new ConnectionManagerImpl(
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
	private JDBCContextImpl() {
	}

	@Override
	public CallbackCoordinator getJtaSynchronizationCallbackCoordinator() {
		return jtaSynchronizationCallbackCoordinator;
	}

	private CallbackCoordinator getJtaSynchronizationCallbackCoordinator(javax.transaction.Transaction jtaTransaction) {
		jtaSynchronizationCallbackCoordinator = new CallbackCoordinator( owner, this, jtaTransaction, hibernateTransaction );
		return jtaSynchronizationCallbackCoordinator;
	}

	@Override
	public void cleanUpJtaSynchronizationCallbackCoordinator() {
		jtaSynchronizationCallbackCoordinator = null;
	}


	// ConnectionManager.Callback implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public void physicalConnectionObtained(Connection connection) {
		if ( owner.getFactory().getStatistics().isStatisticsEnabled() ) {
			owner.getFactory().getStatisticsImplementor().connect();
		}
	}

	@Override
	public void physicalConnectionReleased() {
		if ( !isTransactionCallbackRegistered ) {
			afterTransactionCompletion( false, null );
			// Note : success = false, because we don't know the outcome of the transaction
		}
	}

	@Override
	public void logicalConnectionClosed() {
		// TODO: anything need to be done?
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return owner.getFactory();
	}

	@Override
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public Connection borrowConnection() {
		return connectionManager.borrowConnection();
	}

	public Connection connection() throws HibernateException {
        if (owner.isClosed()) throw new SessionException("Session is closed");
		return connectionManager.getConnection();
	}

	@Override
	public boolean registerCallbackIfNecessary() {
		if ( isTransactionCallbackRegistered ) {
			return false;
		}
        isTransactionCallbackRegistered = true;
        return true;
	}

	@Override
	public boolean registerSynchronizationIfPossible() {
        // we already have a callback registered; either a local
        // (org.hibernate.Transaction) transaction has accepted
        // callback responsibilities, or we have previously
        // registered a transaction synch.
        if (isTransactionCallbackRegistered) return true;
		boolean localCallbacksOnly = owner.getFactory().getSettings()
				.getTransactionFactory()
				.areCallbacksLocalToHibernateTransactions();
        // the configured transaction-factory says it only supports
        // local callback mode, so no sense attempting to register a
        // JTA Synchronization
        if (localCallbacksOnly) return false;
		TransactionManager tm = owner.getFactory().getTransactionManager();
        // if there is no TM configured, we will not be able to access
        // the javax.transaction.Transaction object in order to
        // register a synch anyway.
        if (tm == null) return false;
        try {
            if (!isTransactionInProgress()) {
                LOG.trace("TransactionFactory reported no active transaction; Synchronization not registered");
                return false;
            }
            javax.transaction.Transaction tx = tm.getTransaction();
            if (JTAHelper.isMarkedForRollback(tx)) {
                // transactions marked for rollback-only cause some TM impls to throw exceptions
                LOG.debugf("Transaction is marked for rollback; skipping Synchronization registration");
                return false;
            }
            if (hibernateTransaction == null) hibernateTransaction = owner.getFactory().getSettings().getTransactionFactory().createTransaction(this,
                                                                                                                                                owner);
            tx.registerSynchronization(new HibernateSynchronizationImpl(getJtaSynchronizationCallbackCoordinator(tx)));
//						tx.registerSynchronization( new CacheSynchronization(owner, this, tx, hibernateTransaction) );
            isTransactionCallbackRegistered = true;
            LOG.debugf("Successfully registered Synchronization");
            return true;
        } catch (HibernateException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionException("could not register synchronization with JTA TransactionManager", e);
        }
	}

	@Override
	public boolean isTransactionInProgress() {
		return owner.getFactory().getSettings().getTransactionFactory()
				.isTransactionInProgress( this, owner, hibernateTransaction );
	}

	@Override
	public Transaction getTransaction() throws HibernateException {
		if (hibernateTransaction==null) {
			hibernateTransaction = owner.getFactory().getSettings()
					.getTransactionFactory()
					.createTransaction( this, owner );
		}
		return hibernateTransaction;
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
        LOG.trace("Before transaction completion");
		owner.beforeTransactionCompletion(tx);
	}

	/**
	 * We cannot rely upon this method being called! It is only
	 * called if we are using Hibernate Transaction API.
	 */
	@Override
	public void afterTransactionBegin(Transaction tx) {
        LOG.trace("After transaction begin");
		owner.afterTransactionBegin(tx);
	}

	@Override
	public void afterTransactionCompletion(boolean success, Transaction tx) {
        LOG.trace("After transaction completion");

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
	@Override
	public void afterNontransactionalQuery(boolean success) {
        LOG.trace("After autocommit");
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
			throw owner.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not inspect JDBC autocommit mode"
				);
		}
	}


	// serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isReadyForSerialization() {
		return connectionManager.isReadyForSerialization();
	}

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
	public static JDBCContextImpl deserialize(
			ObjectInputStream ois,
	        Context context,
	        Interceptor interceptor) throws IOException, ClassNotFoundException {
		JDBCContextImpl jdbcContext = new JDBCContextImpl();
		jdbcContext.owner = context;
		jdbcContext.connectionManager = ConnectionManagerImpl.deserialize(
				ois,
				context.getFactory(),
				interceptor,
		        context.getConnectionReleaseMode(),
		        jdbcContext
		);
		return jdbcContext;
	}
}
