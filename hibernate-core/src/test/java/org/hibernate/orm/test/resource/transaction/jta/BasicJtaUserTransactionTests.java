package org.hibernate.orm.test.resource.transaction.jta;

/**
 * @author Steve Ebersole
 */
public class BasicJtaUserTransactionTests extends AbstractBasicJtaTestScenarios {
	@Override
	protected boolean preferUserTransactions() {
		return true;
	}
}
