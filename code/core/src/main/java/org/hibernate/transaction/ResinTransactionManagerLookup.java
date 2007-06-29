//$Id: ResinTransactionManagerLookup.java 3890 2004-06-03 16:31:32Z steveebersole $
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






