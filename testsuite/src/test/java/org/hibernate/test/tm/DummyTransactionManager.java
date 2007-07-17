//$Id: DummyTransactionManager.java 7003 2005-06-03 16:09:59Z steveebersole $
package org.hibernate.test.tm;

import java.util.Properties;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * @author Gavin King
 */
public class DummyTransactionManager implements TransactionManager {

	public static DummyTransactionManager INSTANCE;

	private DummyTransaction current;
	ConnectionProvider connections;
	
	public DummyTransactionManager(Properties props) {
		connections = ConnectionProviderFactory.newConnectionProvider();
	}
	
	public void begin() throws NotSupportedException, SystemException {
		current = new DummyTransaction(this);
		current.begin();
	}

	public void commit() throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException,
			IllegalStateException, SystemException {
		current.commit();
	}


	public int getStatus() throws SystemException {
		return current.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return current;
	}

	public void resume(Transaction tx) throws InvalidTransactionException,
			IllegalStateException, SystemException {
		current = (DummyTransaction) tx;
	}

	public void rollback() throws IllegalStateException, SecurityException,
			SystemException {
		current.rollback();

	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		current.setRollbackOnly();
	}

	public void setTransactionTimeout(int t) throws SystemException {
	}
	
	public Transaction suspend() throws SystemException {
		Transaction result = current;
		current = null;
		return result;
	}

	public DummyTransaction getCurrent() {
		return current;
	}
	
	void endCurrent(DummyTransaction tx) {
		if (current==tx) current=null;
	}

}
