/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.transaction.UserTransaction;

import static com.arjuna.ats.jta.UserTransaction.userTransaction;

@ApplicationScoped
public class UserTransactionProducer {

	@Produces
	@ApplicationScoped
	public UserTransaction createUserTransaction() {
		return userTransaction();
	}
}
