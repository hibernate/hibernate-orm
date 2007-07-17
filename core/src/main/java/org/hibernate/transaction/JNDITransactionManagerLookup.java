//$Id: JNDITransactionManagerLookup.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.transaction;

import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.util.NamingHelper;

/**
 * Locates a <tt>TransactionManager</tt> in JNDI.
 * @author Gavin King
 */
public abstract class JNDITransactionManagerLookup implements TransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.TransactionManagerLookup#getTransactionManager(java.util.Properties)
	 */
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			return (TransactionManager) NamingHelper.getInitialContext(props).lookup( getName() );
		}
		catch (NamingException ne) {
			throw new HibernateException( "Could not locate TransactionManager", ne );
		}
	}

	protected abstract String getName();

}






