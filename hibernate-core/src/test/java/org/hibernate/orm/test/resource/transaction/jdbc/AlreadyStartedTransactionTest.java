/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;


import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13076")
@SessionFactory
public class AlreadyStartedTransactionTest {

	@Test
	@ExpectedException(IllegalStateException.class)
	public void anIllegalStateExceptionShouldBeThrownWhenBeginTxIsCalledWithAnAlreadyActiveTX(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.inSession( (session) -> {
			var transaction = session.getTransaction();
			transaction.begin();
			// A call to begin() with an active Tx should cause an IllegalStateException
			transaction.begin();
		} );
	}
}
