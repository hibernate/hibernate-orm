/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses = MergeWithFinalTest.Thing.class)
class MergeWithFinalTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Thing thing = new Thing( 1L, "hello" );
			session.persist( thing );
		} );
		assertThrows( HibernateException.class,
				() -> scope.inTransaction( session -> {
							final Thing thing = new Thing( 1L, "goodbye" );
							session.merge( thing );
						}
				)
		);
	}

	@Entity
	static class Thing {
		final @Id Long id;
		final String immutable;

		public Thing(Long id, String immutable) {
			this.id = id;
			this.immutable = immutable;
		}

		public Thing() {
			id = null;
			immutable = null;
		}
	}
}
