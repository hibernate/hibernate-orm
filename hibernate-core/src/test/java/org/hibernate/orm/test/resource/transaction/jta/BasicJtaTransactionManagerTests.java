/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
