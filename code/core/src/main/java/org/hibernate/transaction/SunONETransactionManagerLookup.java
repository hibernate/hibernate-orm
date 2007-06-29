//$Id: SunONETransactionManagerLookup.java 11274 2007-03-12 00:18:38Z epbernard $
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for Sun ONE Application Server 7 and above
 * 
 * @author Robert Davidson, Sanjeev Krishnan
 * @author Emmanuel Bernard
 */
public class SunONETransactionManagerLookup extends JNDITransactionManagerLookup {

	protected String getName() {
		return "java:appserver/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
	
}
