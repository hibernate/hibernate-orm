/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stream;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		KeyToOneCollectionFetchScrollTest.BasicEntity.class,
		KeyToOneCollectionFetchScrollTest.EntityA.class,
		KeyToOneCollectionFetchScrollTest.EntityB.class,
		KeyToOneCollectionFetchScrollTest.EntityC.class,
} )
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18476")
public class KeyToOneCollectionFetchScrollTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA a1 = new EntityA("a1");
			final EntityA a2 = new EntityA("a2");
			final EntityB b1 = new EntityB("b1");
			b1.a1 = a1;
			b1.a2 = a2;
			session.persist( a1 );
			session.persist( a2 );
			session.persist( b1 );
			final EntityA a3 = new EntityA("a3");
			final EntityA a4 = new EntityA("a4");
			final EntityB b2 = new EntityB("b2");
			b2.a1 = a3;
			b2.a2 = a4;
			session.persist( a3 );
			session.persist( a4 );
			session.persist( b2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityC" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testScrollWithKeyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try (final Stream<EntityB> stream = session.createQuery(
					"select b from EntityB b join fetch b.a1 join fetch b.a2 left join fetch b.c c order by b.name",
					EntityB.class
			).getResultStream()) {
				final List<EntityB> list = stream.collect( Collectors.toList() );
				assertThat( list ).hasSize( 2 );
				assertThat( list.get( 0 ).getA1() ).isNotNull();
				assertThat( list.get( 0 ).getC() ).hasSize( 0 );
			}
		} );
	}

	@MappedSuperclass
	public static abstract class BasicEntity {
		@Id
		@GeneratedValue
		Long id;
		String name;

		public BasicEntity() {
		}

		public BasicEntity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "EntityA" )
	public static class EntityA extends BasicEntity {

		public EntityA() {
		}

		public EntityA(String name) {
			super( name );
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@ManyToOne
		@JoinColumn( name = "a1_id")
		EntityA a1;
		@Id
		@ManyToOne
		@JoinColumn( name = "a2_id")
		EntityA a2;
		String name;
		@OneToMany( cascade = CascadeType.PERSIST )
		@JoinColumn( name = "entityb_a1_id", referencedColumnName = "a1_id")
		@JoinColumn( name = "entityb_a2_id", referencedColumnName = "a2_id")
		Set<EntityC> c = new HashSet<>();

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}

		public EntityA getA1() {
			return a1;
		}

		public EntityA getA2() {
			return a2;
		}

		public String getName() {
			return name;
		}

		public Set<EntityC> getC() {
			return c;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC extends BasicEntity {
		public EntityC() {
		}

		public EntityC(String name) {
			super( name );
		}
	}
}
