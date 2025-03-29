/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				UnrelatedEntitiesTest.EntityA.class,
				UnrelatedEntitiesTest.EntityB.class
		}
)
@SessionFactory
@JiraKey("HHH-15917")
public class UnrelatedEntitiesTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Set<String> names = new HashSet<>();
					String name = "Fab";
					names.add( name );
					EntityA a = new EntityA( "a", names );
					EntityB b = new EntityB( name );

					session.persist( a );
					session.persist( b );
				}
		);
	}

	@Test
	public void testNoExceptionThrown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<String> resultList = session.createQuery(
							"SELECT 'foobar' FROM EntityA a JOIN EntityB b ON b.name = element(a.names) ",
							String.class
					).getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );
				} );
	}

	@Test
	public void testNoExceptionThrown2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<String> resultList = session.createQuery(
							"SELECT 'foobar' FROM EntityA a JOIN a.names aName JOIN EntityB b ON b.name = aName",
							String.class
					).getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ElementCollection
		private Set<String> names;

		public EntityA() {
		}

		public EntityA(String name, Set<String> names) {
			this.name = name;
			this.names = names;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<String> getNames() {
			return names;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
