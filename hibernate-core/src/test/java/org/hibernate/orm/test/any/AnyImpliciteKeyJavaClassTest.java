/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.Any;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * Test of implicit determination of the key type of @Any.
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {AnyImpliciteKeyJavaClassTest.Book.class})
@JiraKey("HHH-20319")
class AnyImpliciteKeyJavaClassTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
			scope.inTransaction(
					entityManager -> {
						entityManager.persist( new Book() );
						entityManager.flush();
						entityManager.clear();
					}
			);
	}

	@Entity(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private String id;

		@Any
		@JoinColumn(name = "origin_id")
		@Column(name = "origin_type")
		private Object origin;

	}
}
