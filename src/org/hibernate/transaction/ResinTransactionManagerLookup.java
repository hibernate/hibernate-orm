//$Id$
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for Resin
 * @author Aapo Laakkonen
 */
public class ResinTransactionManagerLookup
extends JNDITransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.JNDITransactionManagerLookup#getName()
	 */
	protected String getName() {
		return "java:comp/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

}






