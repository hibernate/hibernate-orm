/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * An eager {@link org.hibernate.annotations.Any} with implicit discriminator values pointing back at
 * its own owner must resolve to that same instance instead of recursing until a {@link StackOverflowError}.
 * <p>
 * Regression from HHH-16730 (7.4): the association is now join-fetched through
 * {@code JoinedDiscriminatedEntityInitializer}, whose {@code internalLoad} fallback re-entered the
 * load of the row being initialized, since it no longer checked the persistence context first.
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = AnyEagerSelfReferenceTest.Document.class)
@JiraKey("HHH-20925")
class AnyEagerSelfReferenceTest {

	@Test
	void testLoadEntityWithEagerAnyPointingAtItself(EntityManagerFactoryScope scope) {

		final Long[] documentId = new Long[1];

		scope.inTransaction(
				entityManager -> {
					Document document = new Document();
					entityManager.persist( document );

					// the @Any points back at the very row that owns it
					document.setTarget( document );

					documentId[0] = document.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					// No HQL: the eager @Any 'target' is join-fetched by the entity loader, resolves to
					// Document, and must be satisfied from the row already being initialized.
					Document document = entityManager.find( Document.class, documentId[0] );

					assertNotNull( document );
					assertSame( document, document.getTarget() );
				}
		);
	}

	@Entity(name = "Document")
	@Table(name = "TDOCUMENT")
	public static class Document implements Referenceable {

		@Id
		@GeneratedValue
		private Long id;

		/**
		 * Eager {@code @Any} without any {@link org.hibernate.annotations.AnyDiscriminatorValue}: the
		 * discriminator values are implicit, so no concrete initializer is registered for the target.
		 */
		@Any(fetch = FetchType.EAGER)
		@AnyKeyJavaClass(Long.class)
		@Column(name = "TARGET_ROLE")
		@JoinColumn(name = "TARGET_ID")
		private Referenceable target;

		public Long getId() {
			return id;
		}

		public Referenceable getTarget() {
			return target;
		}

		public void setTarget(final Referenceable target) {
			this.target = target;
		}
	}

	/**
	 * Declared type of the {@code @Any} ({@link Document#target}).
	 */
	public interface Referenceable {
	}
}
