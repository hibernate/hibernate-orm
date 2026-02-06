/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh12225;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = {
		CaseToOneAssociationTest.Subject.class,
		CaseToOneAssociationTest.Link.class
})
@JiraKey("HHH-16018")
public class CaseToOneAssociationTest {

	private Subject persistedFrom;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		persistedFrom = scope.fromTransaction( em -> {
			final Subject from = new Subject();
			final Subject to = new Subject();
			em.persist( from );
			em.persist( to );
			em.persist( new Link( from, to ) );
			return from;
		} );
	}

	@Test
	public void testUnsupportedCaseForEntity(EntityManagerFactoryScope scope) {
		// Won't catch the AssertionError from assertFound, because it is an Error, not an Exception.
		var iae = Assertions.assertThrows( IllegalArgumentException.class, () -> {
			assertFound(
					scope.fromTransaction( em -> em
							.createQuery(
									"select case when l.from = :from then l.to else l.from end from Link l where from = :from",
									Subject.class
							)
							.setParameter( "from", persistedFrom )
							.getSingleResult() )
			);
		} );
		Assertions.assertInstanceOf( SemanticException.class, iae.getCause(), "IllegalArgumentException wraps a SemanticException" );
		Assertions.assertTrue(
				iae.getCause().getMessage().contains( "CASE only supports returning basic values" ),
				"SE#message talks about CASE"
		);
	}

	private void assertFound(Subject found) {
		Assertions.assertNotEquals( persistedFrom.id, found.id, "Found itself" );
	}

	@Test
	public void testSupportedCaseForJoin(EntityManagerFactoryScope scope) {
		assertFound(
				scope.fromTransaction( em -> em
						.createQuery(
								"""
								select s
								from Link l
								join Subject s ON s.id = case
									when l.from = :from
									then l.to.id
									else l.from.id
								end
								where from = :from
								""",
								Subject.class
						)
						.setParameter( "from", persistedFrom )
						.getSingleResult() )
		);
	}

	@Entity(name = "Subject")
	public static class Subject {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;
	}

	@Entity(name = "Link")
	public static class Link {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		public Subject from;
		@ManyToOne(fetch = FetchType.LAZY)
		public Subject to;

		public Link() {
		}

		public Link(Subject from, Subject to) {
			this.from = from;
			this.to = to;
		}
	}
}
