/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yanming Zhou
 */

@Jpa(annotatedClasses = ReuseQueryWithExpandedParameterTest.Person.class)
@JiraKey("HHH-18027")
public class ReuseQueryWithExpandedParameterTest {

	@Test
	public void reuseQueryWithExpandedParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Person a = new Person( "a" );
			em.persist( a );
			Person b = new Person( "b" );
			em.persist( b );
			Person c = new Person( "c" );
			em.persist( c );
			Person d = new Person( "d" );
			em.persist( d );

			Query q = em.createQuery( "from Person where name in (:names)" );
			assertEquals( 2, q.setParameter( "names", Set.of( "a", "b" ) ).getResultList().size() );
			assertEquals( 2, q.setParameter( "names", Set.of( "c", "d" ) ).getResultList().size() );

			q = em.createQuery( "delete from Person where name in (:names)" );
			assertEquals( 2, q.setParameter( "names", Set.of( "a", "b" ) ).executeUpdate() );
			assertEquals( 2, q.setParameter( "names", Set.of( "c", "d" ) ).executeUpdate() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
