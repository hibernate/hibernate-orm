/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.event.internal.EventListenerLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.logger.LogLevelContext.withLevel;


/**
 * Follow-up to HHH-20229, which rerouted {@link org.hibernate.type.AnyType#toLoggableString}
 * through {@code AnyType.guessEntityPersister(Object, SessionFactoryImplementor)}.
 * <p>
 * That method has a latent bug in its fallback branch: when the {@code @Any} value is an
 * already-initialized {@link org.hibernate.proxy.HibernateProxy} and no {@code EntityNameResolver}
 * matches, the entity name is derived from the still-wrapped proxy argument
 * ({@code object.getClass().getName()}) instead of the unwrapped implementation, producing a name
 * such as {@code ...Book$HibernateProxy} and making {@code getEntityDescriptor(...)} throw
 * {@code UnknownEntityTypeException} while logging flush results.
 * <p>
 * Reaching {@code EntityPrinter#logEntities} requires DEBUG on {@code org.hibernate.orm.event}
 * ({@code AbstractFlushingEventListener#logFlushResults} guard) and DEBUG on
 * {@code org.hibernate.orm.core} ({@code EntityPrinter#logEntities} internal guard).
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {AnyTypeFlushProxyToLoggableStringTest.Book.class})
@JiraKey("HHH-20801")
class AnyTypeFlushProxyToLoggableStringTest {

	@Test
	void testLogEntityWithAnyValueAsInitializedProxy(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new Book( 1L ) ) );

		try (
				var l1 = withLevel( EventListenerLogging.NAME, Logger.Level.DEBUG );
				var l2 = withLevel( CoreMessageLogger.NAME, Logger.Level.DEBUG )
		) {
			scope.inTransaction(
					entityManager -> {
						Book origin = entityManager.getReference( Book.class, 1L );
						// force the proxy to be initialized: guessEntityPersister then skips
						// lazyInitializer.getEntityName() and relies on the buggy fallback
						Hibernate.initialize( origin );

						Book book = new Book( 2L );
						book.setOrigin( origin );
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
		private Long id;

		@Any
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "origin_id")
		@Column(name = "origin_type")
		@AnyDiscriminatorValue(discriminator = "BOOK", entity = Book.class)
		private Object origin;

		public Book() {
		}

		public Book(Long id) {
			this.id = id;
		}

		public Object getOrigin() {
			return origin;
		}

		public void setOrigin(Object origin) {
			this.origin = origin;
		}
	}
}
