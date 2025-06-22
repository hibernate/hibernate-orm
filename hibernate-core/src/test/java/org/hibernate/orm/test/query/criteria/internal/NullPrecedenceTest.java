/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal;

import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Christian Beikov
 */
@JiraKey(value = "HHH-14897")
@Jpa(
		annotatedClasses = { NullPrecedenceTest.Foo.class }
)
public class NullPrecedenceTest {

	@Test
	public void testNullPrecedence(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Foo( 1L, null ) );
			entityManager.persist( new Foo( 2L, "ABC" ) );
			entityManager.persist( new Foo( 3L, "DEF" ) );
			entityManager.persist( new Foo( 4L, "DEF" ) );
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );

			cq.orderBy(
					cb.desc( foo.get( "bar" ), true ),
					cb.desc( foo.get( "id" ) )
			);

			final TypedQuery<Foo> tq = entityManager.createQuery( cq );

			final List<Foo> resultList = tq.getResultList();
			assertEquals( 4, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).getId() );
			assertEquals( 4L, resultList.get( 1 ).getId() );
			assertEquals( 3L, resultList.get( 2 ).getId() );
			assertEquals( 2L, resultList.get( 3 ).getId() );
		} );
	}

	@Entity(name = "Foo")
	public static class Foo {

		private long id;
		private String bar;

		public Foo() {
		}

		public Foo(long id, String bar) {
			this.id = id;
			this.bar = bar;
		}

		@Id
		@Column(nullable = false)
		public long getId() {
			return this.id;
		}

		public void setId(final long id) {
			this.id = id;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}
}
