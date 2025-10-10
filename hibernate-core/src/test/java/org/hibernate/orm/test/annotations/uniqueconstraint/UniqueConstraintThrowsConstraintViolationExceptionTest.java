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
import jakarta.persistence.Table;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11236")
@DomainModel(
		annotatedClasses = {
				UniqueConstraintThrowsConstraintViolationExceptionTest.Customer.class
		}
)
@SessionFactory
public class UniqueConstraintThrowsConstraintViolationExceptionTest {


	@Test
	public void testUniqueConstraintWithEmptyColumnName(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Customer customer1 = new Customer();
			customer1.customerId = "123";
			session.persist( customer1 );
		} );

		assertThrows( ConstraintViolationException.class, () -> scope.inTransaction( session -> {
			Customer customer1 = new Customer();
			customer1.customerId = "123";
			session.persist( customer1 );
		} ) );
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
