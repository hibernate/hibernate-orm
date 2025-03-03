/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CustomAfterCompletionTest extends BaseCoreFunctionalTestCase {

	@Test
	@JiraKey(value = "HHH-13666")
	public void success() {
		inSession( session -> {
			AtomicBoolean called = new AtomicBoolean( false );
			session.getActionQueue().registerProcess( new AfterTransactionCompletionProcess() {
				@Override
				public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
					called.set( true );
				}
			} );
			Assert.assertFalse( called.get() );
			inTransaction( session, theSession -> {
				theSession.persist( new SimpleEntity( "jack" ) );
			} );
			Assert.assertTrue( called.get() );
		} );

		// Check that the transaction was committed
		inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
					.uniqueResult();
			assertEquals( 1L, count );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13666")
	public void failure() {
		try {
			inSession( session -> {
				session.getActionQueue().registerProcess( new AfterTransactionCompletionProcess() {
					@Override
					public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
						throw new RuntimeException( "My exception" );
					}
				} );
				inTransaction( session, theSession -> {
					theSession.persist( new SimpleEntity( "jack" ) );
				} );
			} );
			Assert.fail( "Expected exception to be thrown" );
		}
		catch (Exception e) {
			assertThat( e, instanceOf( HibernateException.class ) );
			assertThat( e.getMessage(), containsString( "Unable to perform afterTransactionCompletion callback" ) );
			Throwable cause = e.getCause();
			assertThat( cause, instanceOf( RuntimeException.class ) );
			assertThat( cause.getMessage(), containsString( "My exception" ) );

			// Make sure that the original message is appended to the wrapping exception's message, for convenience
			assertThat( e.getMessage(), containsString( "My exception" ) );
		}

		// Check that the transaction was committed
		inTransaction( session -> {
			long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
					.uniqueResult();
			assertEquals( 1L, count );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		SimpleEntity() {
		}

		SimpleEntity(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
