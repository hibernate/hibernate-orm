/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11236")
public class UniqueConstraintThrowsConstraintViolationExceptionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class };
	}

	protected void configure(Configuration configuration) {
		configuration.setProperty( Environment.HBM2DDL_AUTO, "update" );
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
					e.getCause().getClass()
			);
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity
	@Table(name = "CUSTOMER")
	public static class Customer {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic
		@Column(name = "CUSTOMER_ID", unique = true)
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public Customer() {
		}
	}
}
