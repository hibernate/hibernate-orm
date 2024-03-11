/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceCircularBiDirectionalFetchTest.Animal.class,
		JoinedInheritanceCircularBiDirectionalFetchTest.Cat.class,
		JoinedInheritanceCircularBiDirectionalFetchTest.Leg.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17832" )
public class JoinedInheritanceCircularBiDirectionalFetchTest {
	@Test
	public void testJoinSelectCat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Cat result = session.createQuery(
					"select cat from Cat cat join cat.legs leg",
					Cat.class
			).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "gatta" );
			assertThat( result.getLegs() ).hasSize( 2 ).extracting( Leg::getCat ).containsOnly( result );
		} );
	}

	@Test
	public void testJoinSelectLeg(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Leg> resultList = session.createQuery(
					"select leg from Cat cat join cat.legs leg",
					Leg.class
			).getResultList();
			assertThat( resultList )
					.hasSize( 2 ).allMatch( l -> Hibernate.isInitialized( l.getCat() ) )
					.extracting( Leg::getCat ).containsOnly( resultList.get( 0 ).getCat() )
					.extracting( Cat::getName ).containsOnly( "gatta" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Leg leg1 = new Leg( "leg1" );
			final Leg leg2 = new Leg( "leg2" );
			final Cat cat = new Cat();
			cat.setName( "gatta" );
			cat.addToLegs( leg1 );
			cat.addToLegs( leg2 );
			session.persist( cat );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Leg" ).executeUpdate();
			session.createMutationQuery( "delete from Animal" ).executeUpdate();
		} );
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Animal {
		@Id
		@GeneratedValue
		protected Long id;

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

	@Entity( name = "Cat" )
	public static class Cat extends Animal {
		@OneToMany( mappedBy = "cat", cascade = CascadeType.PERSIST )
		private List<Leg> legs = new LinkedList<>();

		public List<Leg> getLegs() {
			return this.legs;
		}

		public void addToLegs(Leg person) {
			legs.add( person );
			person.cat = this;
		}
	}

	@Entity( name = "Leg" )
	public static class Leg {
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private Cat cat;

		private String name;

		public Leg() {
		}

		public Leg(String name) {
			this.name = name;
		}

		public Cat getCat() {
			return cat;
		}
	}
}
