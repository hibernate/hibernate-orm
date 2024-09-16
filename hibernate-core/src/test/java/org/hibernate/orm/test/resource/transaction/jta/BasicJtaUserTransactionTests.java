/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
