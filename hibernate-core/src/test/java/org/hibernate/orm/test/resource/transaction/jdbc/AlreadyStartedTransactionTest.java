/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13076")
public class AlreadyStartedTransactionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test(expected = IllegalStateException.class)
	public void anIllegalStateExceptionShouldBeThrownWhenBeginTxIsCalledWithAnAlreadyActiveTX() {
		Transaction transaction = null;
		try (Session session = openSession()) {
			transaction = session.getTransaction();
			transaction.begin();
			// A call to begin() with an active Tx should cause an IllegalStateException
			transaction.begin();
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
		}
	}
}
