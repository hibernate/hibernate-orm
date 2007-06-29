//$Id: OrionTransactionManagerLookup.java 3890 2004-06-03 16:31:32Z steveebersole $
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






