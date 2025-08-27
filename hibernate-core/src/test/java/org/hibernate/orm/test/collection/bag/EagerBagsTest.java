/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.Collection;
import java.util.LinkedList;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				EagerBagsTest.EntityA.class,
				EagerBagsTest.EntityB.class,
				EagerBagsTest.EntityC.class
		}
)
@SessionFactory
public class EagerBagsTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC c = new EntityC( 1l, "c" );
					EntityC c1 = new EntityC( 2l, "c1" );
					EntityC c2 = new EntityC( 3l, "c2" );

					EntityC c3 = new EntityC( 4l, "c3" );

					EntityB b = new EntityB( 1l, "b" );

					b.addAttribute( c );
					b.addAttribute( c1 );

					EntityB b1 = new EntityB( 2l, "b1" );

					b1.addAttribute( c2 );
					b1.addAttribute( c3 );

					EntityA a = new EntityA( 1l, "a" );

					a.addAttribute( b );
					a.addAttribute( b1 );

					session.persist( c );
					session.persist( c1 );
					session.persist( c2 );
					session.persist( c3 );

					session.persist( b );
					session.persist( b1 );

					session.persist( a );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, 1l );
					assertThat( entityA.attributes.size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		Collection<EntityB> attributes = new LinkedList<>();

		public EntityA() {
		}

		public EntityA(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Collection<EntityB> getAttributes() {
			return attributes;
		}

		public void addAttribute(EntityB entityB) {
			attributes.add( entityB );
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		Collection<EntityC> attributes = new LinkedList<>();


		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Collection<EntityC> getAttributes() {
			return attributes;
		}

		public void addAttribute(EntityC entityC) {
			this.attributes.add( entityC );
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		private Long id;

		private String name;

		public EntityC() {
		}

		public EntityC(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
