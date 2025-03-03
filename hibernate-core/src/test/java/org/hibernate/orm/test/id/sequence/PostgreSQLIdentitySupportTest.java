/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mhalcea
 */
@JiraKey(value = "HHH-13202")
@RequiresDialect(value = PostgreSQLDialect.class)
@Jpa(
		annotatedClasses = PostgreSQLIdentitySupportTest.Role.class
)
public class PostgreSQLIdentitySupportTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Role _role = scope.fromTransaction( entityManager -> {
			Role role = new Role();

			entityManager.persist( role );

			return role;
		} );

		scope.inTransaction( entityManager -> {
			Role role = entityManager.find( Role.class, _role.getId() );
			assertNotNull( role );
		} );
	}

	@Entity(name = "Role")
	public static class Role {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}

}
