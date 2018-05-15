/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.resource.transaction.jdbc;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13076")
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
