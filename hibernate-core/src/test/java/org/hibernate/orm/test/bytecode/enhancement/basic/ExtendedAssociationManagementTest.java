/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class ExtendedAssociationManagementTest {

	@Test
	public void test() {
		User user = new User();
		user.login = SafeRandomUUIDGenerator.safeRandomUUIDAsString();

		Customer customer = new Customer();
		customer.user = user;

		assertEquals( customer, getFieldByReflection( user, "customer" ) );

		// check dirty tracking is set automatically with bi-directional association management
		EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

		User anotherUser = new User();
		anotherUser.login = SafeRandomUUIDGenerator.safeRandomUUIDAsString();

		customer.user = anotherUser;

		assertNull( user.customer );
		assertEquals( customer, getFieldByReflection( anotherUser, "customer" ) );

		user.customer = new Customer();
		assertEquals( user, user.customer.user );
	}

	// --- //

	@Entity
	private static class Customer {

		@Id
		Long id;

		String firstName;

		String lastName;

		@OneToOne( fetch = FetchType.LAZY )
		User user;
	}

	@Entity
	private static class User {

		@Id
		Long id;

		String login;

		String password;

		@OneToOne( mappedBy = "user", fetch = FetchType.LAZY )
		Customer customer;
	}
}
