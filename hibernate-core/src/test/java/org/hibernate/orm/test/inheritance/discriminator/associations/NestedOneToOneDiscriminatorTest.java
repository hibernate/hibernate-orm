/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.associations;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		NestedOneToOneDiscriminatorTest.Person.class,
		NestedOneToOneDiscriminatorTest.BodyPart.class,
		NestedOneToOneDiscriminatorTest.Leg.class,
		NestedOneToOneDiscriminatorTest.NestedEntity.class
})
public class NestedOneToOneDiscriminatorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = new Person();
			person.setName( "initialName" );
			person.getLegs().add( new Leg( "left leg", person, new NestedEntity( "nested in left leg" ) ) );
			session.persist( person );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from BodyPart" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
			session.createMutationQuery( "delete from NestedEntity" ).executeUpdate();
		} );
	}

	@Test
	@JiraKey("HHH-16037")
	public void testUpdatePerson(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = session.find( Person.class, 1L );
			assertEquals( "initialName", person.getName() );
			assertEquals( 1L, person.getId() );
			person.setName( "changedName" );
		} );
		scope.inTransaction( session -> {
			final Person person = session.find( Person.class, 1L );
			assertEquals( "changedName", person.getName() );
			assertEquals( 1L, person.getId() );
		} );
	}

	@Test
	@JiraKey("HHH-16053")
	public void testJoinFetchNested(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			String jpql = "select person " +
					"from Person person " +
					"left join fetch person.legs as leg " +
					"left join fetch leg.nestedEntity " +
					"where person.id = :id";
			final Person person = session.createQuery( jpql, Person.class )
					.setParameter( "id", 1L )
					.getSingleResult();
			assertEquals( 1L, person.getId() );
			assertTrue( Hibernate.isInitialized( person.getLegs() ) );
			final Leg leg = person.getLegs().iterator().next();
			assertEquals( "left leg", leg.getName() );
			assertTrue( Hibernate.isInitialized( leg.getNestedEntity() ) );
			assertEquals( "nested in left leg", leg.getNestedEntity().getName() );
		} );
	}

	@MappedSuperclass
	public abstract static class BaseEntity {
		@Id
		@GeneratedValue
		private Long id;

		protected String name;

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Table(name = "person")
	public static class Person extends BaseEntity {
		@OneToMany(fetch = FetchType.EAGER, mappedBy = "person", cascade = CascadeType.ALL)
		private Set<Leg> legs = new HashSet<>();

		public Set<Leg> getLegs() {
			return legs;
		}
	}

	@Entity(name = "BodyPart")
	@Table(name = "body_part")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "discriminator")
	public abstract static class BodyPart extends BaseEntity {
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		protected NestedEntity nestedEntity;

		public NestedEntity getNestedEntity() {
			return nestedEntity;
		}
	}

	@Entity(name = "Leg")
	@DiscriminatorValue("LegBodyPart")
	public static class Leg extends BodyPart {
		@ManyToOne(fetch = FetchType.EAGER, optional = false)
		private Person person;

		public Leg() {
		}

		public Leg(String name, Person person, NestedEntity nestedEntity) {
			this.name = name;
			this.person = person;
			this.nestedEntity = nestedEntity;
		}

		public Person getPerson() {
			return person;
		}
	}

	@Entity(name = "NestedEntity")
	@Table(name = "nested_entity")
	public static class NestedEntity extends BaseEntity {
		public NestedEntity() {
		}

		public NestedEntity(String name) {
			this.name = name;
		}
	}
}
