/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test behavior of transaction timeouts
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = TransactionTimeoutTests.Person.class)
@SessionFactory
public class TransactionTimeoutTests {
	@Test
	void testJdbcTxn(SessionFactoryScope sessions) {
		sessions.inSession( (session) -> {
			final Transaction transaction = session.getTransaction();
			// timeout in 2 seconds
			transaction.setTimeout( 2 );

			// start the transaction and sleep for 3 seconds to exceed the transaction timeout
			transaction.begin();
			try {
				Thread.sleep( 3 * 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( "Thread#sleep error", e );
			}
			try {
				// perform an operation against the db and try to commit the transaction
				session.createSelectionQuery( "from Person", Person.class ).list();
				transaction.commit();
				fail( "Transaction should have timed out" );
			}
			catch (PersistenceException e) {
				assertThat( e ).isInstanceOf( TransactionException.class );
				assertThat( e ).hasMessageContaining( "Transaction timeout expired" );
			}
		} );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}
}
