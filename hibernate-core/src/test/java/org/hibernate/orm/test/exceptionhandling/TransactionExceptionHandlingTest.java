/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Transaction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-12666")
@RequiresDialect(H2Dialect.class)
public class TransactionExceptionHandlingTest extends BaseExceptionHandlingTest {

	public TransactionExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionExpectations );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, B.class };
	}

	@AfterEach
	void tearDown() {
		//noinspection resource
		sessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testPersistWithGeneratedValue() {
		//noinspection resource
		try (var s = sessionFactory().openSession()) {
			// Get the transaction and set the timeout BEFORE calling begin()
			Transaction t = s.getTransaction();
			t.setTimeout( 1 );
			Assertions.assertEquals( -1, s.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );

			TransactionUtil.inTransaction( s, (SessionImplementor s2) -> {
				wait(1000);
				try {
					s.persist( new A() );
					Assertions.fail( "should have thrown an exception" );
				}
				catch (RuntimeException expected){
					exceptionExpectations.onTransactionExceptionOnPersistAndMergeAndFlush( expected );
				}
			} );
		}
	}

	private void wait(int millis) {
		try {
			Thread.sleep( millis );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	@Test
	public void testMergeWithGeneratedValue() {
		//noinspection resource
		try (var s = sessionFactory().openSession()) {
			// Get the transaction and set the timeout BEFORE calling begin()
			Transaction t = s.getTransaction();
			t.setTimeout( 1 );
			Assertions.assertEquals( -1, s.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );

			TransactionUtil.inTransaction( s, (SessionImplementor s2) -> {
				wait( 1000 );
				try {
					s.merge( new A() );
					Assertions.fail( "should have thrown an exception" );
				}
				catch (RuntimeException expected){
					exceptionExpectations.onTransactionExceptionOnPersistAndMergeAndFlush( expected );
				}
			} );
		}
	}

	@Test
	public void testSaveWithGeneratedValue() {
		//noinspection resource
		try (var s = sessionFactory().openSession()) {
			// Get the transaction and set the timeout BEFORE calling begin()
			Transaction t = s.getTransaction();
			t.setTimeout( 1 );
			Assertions.assertEquals( -1, s.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );

			TransactionUtil.inTransaction( s, (SessionImplementor s2) -> {
				wait( 1000 );
				try {
					s.persist( new A() );
					Assertions.fail( "should have thrown an exception" );
				}
				catch (RuntimeException expected){
					exceptionExpectations.onTransactionExceptionOnPersistAndMergeAndFlush( expected );
				}
			} );
		}
	}

	@Test
	public void testFlushWithAssignedValue() {
		//noinspection resource
		try (var s = sessionFactory().openSession()) {
			// Get the transaction and set the timeout BEFORE calling begin()
			Transaction t = s.getTransaction();
			t.setTimeout( 1 );
			Assertions.assertEquals( -1, s.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );

			TransactionUtil.inTransaction( s, (SessionImplementor s2) -> {
				wait(1000);
				try {
					s.persist( new B( 1 ) );
					s.flush();
					Assertions.fail( "should have thrown an exception" );
				}
				catch (RuntimeException expected){
					exceptionExpectations.onTransactionExceptionOnPersistAndMergeAndFlush( expected );
				}
			} );
		}
	}

	@Test
	public void testCommitWithAssignedValue() {
		//noinspection resource
		try (var s = sessionFactory().openSession()) {
			// Get the transaction and set the timeout BEFORE calling begin()
			Transaction t = s.getTransaction();
			t.setTimeout( 1 );
			Assertions.assertEquals( -1, s.getJdbcCoordinator().determineRemainingTransactionTimeOutPeriod() );

			t.begin();
			wait(1000);
			try {
				s.persist( new B( 1 ) );
				s.getTransaction().commit();
				Assertions.fail( "should have thrown an exception" );
			}
			catch (RuntimeException expected){
				exceptionExpectations.onTransactionExceptionOnCommit( expected );
			}
			finally {
				t.rollback();
			}
		}
	}

	@SuppressWarnings("unused")
	@Entity(name = "A")
	public static class A {
		@Id
		@GeneratedValue
		private long id;
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name = "B")
	public static class B {
		@Id
		private long id;

		public B() {
		}

		public B(long id) {
			this.id = id;
		}
	}

}
