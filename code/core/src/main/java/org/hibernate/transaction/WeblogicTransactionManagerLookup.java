//$Id: WeblogicTransactionManagerLookup.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for WebLogic
 * @author Gavin King
 */
public final class WeblogicTransactionManagerLookup extends JNDITransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.JNDITransactionManagerLookup#getName()
	 */
	protected String getName() {
		return "javax.transaction.TransactionManager";
	}

	public String getUserTransactionName() {
		return "javax.transaction.UserTransaction";
	}

}






