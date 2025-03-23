/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11236")
public class UniqueConstraintThrowsConstraintViolationExceptionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class };
	}

	@Test
	public void testUniqueConstraintWithEmptyColumnName() {
		doInHibernate( this::sessionFactory, session -> {
			Customer customer1 = new Customer();
			customer1.customerId = "123";
			session.persist( customer1 );
		} );
		try {
			doInHibernate( this::sessionFactory, session -> {
				Customer customer1 = new Customer();
				customer1.customerId = "123";
				session.persist( customer1 );
			} );
			fail( "Should throw" );
		}
		catch ( PersistenceException e ) {
			assertEquals(
					ConstraintViolationException.class,
					e.getClass()
			);
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER")
	public static class Customer {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic(optional = false)
		@Column(name = "CUSTOMER_ID", unique = true)
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public Customer() {
		}
	}
}
