package org.hibernate.transaction;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

/**
 * TransactionManager lookup strategy for BTM
 * @author Ludovic Orban
 */
public class BTMTransactionManagerLookup implements TransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.TransactionManagerLookup#getTransactionManager(Properties)
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			Class clazz = Class.forName("bitronix.tm.TransactionManagerServices");
			return (TransactionManager) clazz.getMethod("getTransactionManager", null).invoke(null, null);
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain BTM transaction manager instance", e );
		}
	}

	/**
	 * @see org.hibernate.transaction.TransactionManagerLookup#getUserTransactionName()
	 */
	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
}