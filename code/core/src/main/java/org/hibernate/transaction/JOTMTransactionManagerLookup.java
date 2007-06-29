package org.hibernate.transaction;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

/**
 * TransactionManager lookup strategy for JOTM
 * @author Low Heng Sin
 */
public class JOTMTransactionManagerLookup implements TransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.TransactionManagerLookup#getTransactionManager(Properties)
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			Class clazz = Class.forName("org.objectweb.jotm.Current");
			return (TransactionManager) clazz.getMethod("getTransactionManager", null).invoke(null, null);
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain JOTM transaction manager instance", e );
		}
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

}






