/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * Test case reproducing an issue in EntityPrinter.logEntities during logFlushResults,
 * where a HibernateException is thrown while attempting to wrap an entity as a String.
 *
 * The failure occurs when an @Any association uses a String identifier via @AnyKeyJavaClass(String.class),
 * leading Hibernate to incorrectly attempt a conversion using StringJavaType.
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {FlushEntityWithAnyKeyJavaClassAsStringTest.Book.class})
@JiraKey("HHH-20229")
class FlushEntityWithAnyKeyJavaClassAsStringTest {

	@Test
	void testLogEntityWithAnyKeyJavaClassAsString(EntityManagerFactoryScope scope) {

		LoggerContext context = (LoggerContext) LogManager.getContext( false );
		Configuration configuration = context.getConfiguration();

		LoggerConfig coreLogger = configuration.getLoggerConfig( "org.hibernate.orm.core" );
		LoggerConfig eventLogger = configuration.getLoggerConfig( "org.hibernate.orm.event" );

		Level oldCoreLogLevel = coreLogger.getLevel();
		Level oldEventLogLevel = eventLogger.getLevel();

		coreLogger.setLevel(Level.DEBUG);
		eventLogger.setLevel( Level.DEBUG );

		context.updateLoggers();
		try {
			scope.inTransaction(
					entityManager -> {
						Book book = new Book();
						Book origin = new Book();
						book.setOrigin(  origin);
						entityManager.persist( origin );
						entityManager.persist( book );
						entityManager.flush();
						entityManager.clear();
					}
			);
		}
		finally {
			coreLogger.setLevel( oldCoreLogLevel );
			eventLogger.setLevel( oldEventLogLevel );
			context.updateLoggers();
		}
	}

	@Entity(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private String id;

		@Any
		@AnyKeyJavaClass(String.class)
		@JoinColumn(name="origin_id")
		@Column(name="origin_type")
		@AnyDiscriminatorValue(discriminator="BOOK", entity=Book.class)
		private Object origin;

		public Object getOrigin() {
			return origin;
		}

		public void setOrigin(Object origin) {
			this.origin = origin;
		}
	}
}
