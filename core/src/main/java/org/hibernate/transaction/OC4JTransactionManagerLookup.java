package org.hibernate.transaction;

/**
 * TransactionManagerLookup for the OC4J (Oracle) app-server.
 * 
 * @author Stijn Janssens
 */
public class OC4JTransactionManagerLookup extends JNDITransactionManagerLookup {
	protected String getName() {
		return "java:comp/pm/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
}
