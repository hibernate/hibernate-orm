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
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.logger.LogLevelContext.withLevel;


/**
 * Test case reproducing an issue in EntityPrinter.logEntities during logFlushResults,
 * where a HibernateException is thrown while attempting to wrap an entity as a String.
 * <p>
 * The failure occurs when an @Any association uses a String identifier via @AnyKeyJavaClass(String.class),
 * leading Hibernate to incorrectly attempt a conversion using StringJavaType.
 *
 * @author Vincent Bouthinon
 * @author Utsav Mehta
 */
@Jpa(annotatedClasses = {AnyTypeFlushToLoggableStringTest.Book.class})
@JiraKey("HHH-20229")
class AnyTypeFlushToLoggableStringTest {

	@Test
	void testLogEntityWithAnyKeyJavaClassAsString(EntityManagerFactoryScope scope) {
		try (
				var l1 = withLevel( CoreMessageLogger.NAME, Logger.Level.DEBUG );
		) {
			scope.inTransaction(
					entityManager -> {
						Book book = new Book();
						Book origin = new Book();
						book.setOrigin( origin );
						entityManager.persist( origin );
						entityManager.persist( book );
						entityManager.flush();
						entityManager.clear();
					}
			);
		}
	}

	@Entity(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private String id;

		@Any
		@AnyKeyJavaClass(String.class)
		@JoinColumn(name = "origin_id")
		@Column(name = "origin_type")
		@AnyDiscriminatorValue(discriminator = "BOOK", entity = Book.class)
		private Object origin;

		public Object getOrigin() {
			return origin;
		}

		public void setOrigin(Object origin) {
			this.origin = origin;
		}
	}
}
