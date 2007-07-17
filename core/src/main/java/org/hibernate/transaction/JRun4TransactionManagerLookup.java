package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for JRun4
 * @author Joseph Bissen
 */
public class JRun4TransactionManagerLookup extends JNDITransactionManagerLookup {

	protected String getName() {
		return "java:/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
}
