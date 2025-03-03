/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.associations;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		OneToManyJoinFetchDiscriminatorTest.Person.class,
		OneToManyJoinFetchDiscriminatorTest.BodyPart.class,
		OneToManyJoinFetchDiscriminatorTest.Leg.class,
		OneToManyJoinFetchDiscriminatorTest.Arm.class,
})
@JiraKey("HHH-16157")
public class OneToManyJoinFetchDiscriminatorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = new Person();
			person.setName( "initialName" );
			person.getLegs().add( new Leg( "left leg", person ) );
			session.persist( person );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from BodyPart" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Test
	public void testJoinFetchDiscriminatorCondition(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Person person = session.createQuery( "from Person person left join fetch person.legs", Person.class )
					.getSingleResult();
			assertTrue( Hibernate.isInitialized( person.getLegs() ) );
			assertEquals( 1, person.getLegs().size() );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "LegBodyPart", 1 );
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
		@OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL)
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
	}

	@Entity(name = "Leg")
	@DiscriminatorValue("LegBodyPart")
	public static class Leg extends BodyPart {
		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Person person;

		public Leg() {
		}

		public Leg(String name, Person person) {
			this.name = name;
			this.person = person;
		}

		public Person getPerson() {
			return person;
		}
	}

	@Entity(name = "Arm")
	@DiscriminatorValue("ArmBodyPart")
	public static class Arm extends BodyPart {
		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Person person;

		public Arm() {
		}

		public Arm(String name, Person person) {
			this.name = name;
			this.person = person;
		}

		public Person getPerson() {
			return person;
		}
	}
}
