/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
import static org.junit.Assert.assertTrue;


@DomainModel(
		annotatedClasses = {
				BagAndSetFetchTest.EntityA.class,
				BagAndSetFetchTest.EntityB.class,
				BagAndSetFetchTest.EntityC.class,
				BagAndSetFetchTest.EntityD.class,
		}
)
@SessionFactory
public class BagAndSetFetchTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC c = new EntityC( 1l, "c" );
					EntityC c1 = new EntityC( 2l, "c1" );

					EntityD d = new EntityD( 1l, "d" );
					EntityD d1 = new EntityD( 2l, "d1" );
					EntityD d2 = new EntityD( 3l, "d2" );

					EntityB b = new EntityB( 1l, "b" );

					b.addAttribute( c );
					b.addAttribute( c1 );

					b.addEntityD( d );
					b.addEntityD( d1 );
					b.addEntityD( d2 );

					EntityB b1 = new EntityB( 2l, "b1" );

					EntityC c2 = new EntityC( 3l, "c2" );

					EntityC c3 = new EntityC( 4l, "c3" );
					EntityC c4 = new EntityC( 5l, "c4" );

					b1.addAttribute( c2 );
					b1.addAttribute( c3 );
					b1.addAttribute( c4 );

					EntityA a = new EntityA( 1l, "a" );

					a.addAttribute( b );
					a.addAttribute( b1 );

					session.persist( c );
					session.persist( c1 );
					session.persist( c2 );
					session.persist( c3 );
					session.persist( c4 );

					session.persist( d );
					session.persist( d1 );
					session.persist( d2 );

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
					EntityB entityB = session.find( EntityB.class, 1l );
					Collection<EntityC> entityCS = entityB.getAttributes();
					Set<EntityD> entityDS = entityB.entityDS;
					assertThat( entityB.getName() ).isEqualTo( "b" );
					assertThat( entityCS.size() ).isEqualTo( 2 );
					assertThat( entityDS.size() ).isEqualTo( 3 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, 1l );
					Collection<EntityB> attributes = entityA.attributes;
					assertThat( attributes.size() ).isEqualTo( 2 );
					boolean findB = false;
					boolean findB1 = false;
					for ( EntityB entityB : attributes ) {
						Collection<EntityC> entityCS = entityB.attributes;
						Set<EntityD> entityDS = entityB.entityDS;
						if ( entityB.getName().equals( "b" ) ) {
							assertThat( entityCS.size() ).isEqualTo( 2 );
							assertThat( entityDS.size() ).isEqualTo( 3 );
							findB = true;
						}
						else {
							assertThat( entityCS.size() ).isEqualTo( 3 );
							assertThat( entityDS.size() ).isEqualTo( 0 );
							findB1 = true;
						}
					}
					assertTrue( findB );
					assertTrue( findB1 );
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

		@OneToMany(fetch = FetchType.EAGER)
		Set<EntityD> entityDS = new HashSet<>();

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

		public void addEntityD(EntityD entityD) {
			this.entityDS.add( entityD );
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

	@Entity(name = "EntityD")
	public static class EntityD {
		@Id
		private Long id;

		private String name;

		public EntityD() {
		}

		public EntityD(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
