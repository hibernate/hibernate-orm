/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Add test which composes not with in. Test introduced after discovery the negated in
 * fails under dialects without record-level construction, such as DB2.
 *
 * @author Mike Mannion
 */
@JiraKey(value = "HHH-19497")
@DomainModel(
		annotatedClasses = {Foo.class, FooType.class}
)
@SessionFactory
class NegatedInPredicateTest {

	public static final FooType FOO_TYPE1 = new FooType( "ft1", "ctx1" );
	public static final FooType FOO_TYPE2 = new FooType( "ft2", "ctx1" );

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.persist( FOO_TYPE1 );
					em.persist( FOO_TYPE2 );

					Foo foo1 = new Foo( 1L, FOO_TYPE1 );
					Foo foo2 = new Foo( 2L, FOO_TYPE1 );
					Foo foo3 = new Foo( 3L, FOO_TYPE2 );

					em.persist( foo1 );
					em.persist( foo2 );
					em.persist( foo3 );
				}
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.createQuery( "delete from Foo" ).executeUpdate();
					em.createQuery( "delete from FooType" ).executeUpdate();
				}
		);
	}

	@Test
	void testSanity(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder cb = em.getCriteriaBuilder();
					CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
					Root<Foo> root = cq.from( Foo.class );
					assertThat( em.createQuery( cq.select( root ) ).getResultList() )
							.hasSize( 3 );
				}
		);
	}

	@Test
	void testNegatedPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder cb = em.getCriteriaBuilder();
					CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
					Root<Foo> root = cq.from( Foo.class );
					cq.select( root )
							.where( cb.not( root.get( "fooType" ).in( List.of( FOO_TYPE1, FOO_TYPE2 ) ) ) );
					assertThat( em.createQuery( cq ).getResultList() )
							.isEmpty();
				}
		);
	}

	@Test
	void testNonNegatedInPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder cb = em.getCriteriaBuilder();
					CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
					Root<Foo> root = cq.from( Foo.class );
					cq.select( root )
							.where( root.get( "fooType" ).in( List.of( FOO_TYPE1, FOO_TYPE2 ) ) );
					assertThat( em.createQuery( cq ).getResultList() )
							.hasSize( 3 );

				}
		);
	}

}
