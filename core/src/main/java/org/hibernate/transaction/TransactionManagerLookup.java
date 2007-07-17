//$Id: TransactionManagerLookup.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.transaction;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;

/**
 * Concrete implementations locate and return the JTA
 * <tt>TransactionManager</tt>.
 * @author Gavin King
 */
public interface TransactionManagerLookup {

	/**
	 * Obtain the JTA <tt>TransactionManager</tt>
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException;

	/**
	 * Return the JNDI name of the JTA <tt>UserTransaction</tt>
	 * or <tt>null</tt> (optional operation).
	 */
	public String getUserTransactionName();

}






