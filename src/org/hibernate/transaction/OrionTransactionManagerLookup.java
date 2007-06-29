//$Id$
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for Orion
 * @author Gavin King
 */
public class OrionTransactionManagerLookup
extends JNDITransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.JNDITransactionManagerLookup#getName()
	 */
	protected String getName() {
		return "java:comp/UserTransaction";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

}






