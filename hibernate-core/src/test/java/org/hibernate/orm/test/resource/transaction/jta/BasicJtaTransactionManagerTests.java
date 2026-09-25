package org.hibernate.orm.test.resource.transaction.jta;

/**
 * @author Steve Ebersole
 */
public class BasicJtaTransactionManagerTests extends AbstractBasicJtaTestScenarios {
	@Override
	protected boolean preferUserTransactions() {
		return false;
	}
}
