/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.CurrentTimestamp;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GeneratedNoOpUpdateTest.Pizza.class,
		GeneratedNoOpUpdateTest.Topping.class,
} )
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-18484" )
public class GeneratedNoOpUpdateTest {
	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		final ZonedDateTime updatedTime = scope.fromTransaction( session -> {
			final Pizza pizza = session.find( Pizza.class, 1L );
			final ZonedDateTime initialTime = pizza.getLastUpdated();
			// Create a new topping
			final Topping newTopping1 = new Topping();
			newTopping1.setName( "Cheese" );
			newTopping1.setPizza( pizza );
			// Let's mutate the existing list
			pizza.getToppings().add( newTopping1 );
			session.flush();
			// pizza was not dirty so no update is executed
			inspector.assertNoUpdate();
			assertThat( pizza.getLastUpdated() ).isEqualTo( initialTime );
			return pizza.getLastUpdated();
		} );

		inspector.clear();
		scope.inTransaction( session -> {
			// Now let's try adding a new topping via a new list
			final Pizza pizza = session.find( Pizza.class, 1L );
			// Create a new topping
			final Topping newTopping2 = new Topping();
			newTopping2.setName( "Mushroom" );
			newTopping2.setPizza( pizza );
			// This time, instead of mutating the existing list, we're creating a new list
			pizza.setToppings( List.of( pizza.getToppings().get( 0 ), newTopping2 ) );
			session.flush();
			// pizza this time was dirty, but still no update is executed because
			// only the unowned one-to-many association has changed
			inspector.assertNoUpdate();
			assertThat( pizza.getLastUpdated() ).isEqualTo( updatedTime );
		} );

		scope.inTransaction( session -> {
			final Pizza pizza = session.find( Pizza.class, 1L );
			assertThat( pizza.getToppings() ).hasSize( 3 )
					.extracting( Topping::getName )
					.containsExactlyInAnyOrder( "Pepperoni", "Cheese", "Mushroom" );
			// This time we mutate the pizza to trigger a real update
			pizza.setName( "Salamino e funghi" );
			session.flush();
			assertThat( inspector.getSqlQueries() ).anyMatch( sql -> sql.toLowerCase( Locale.ROOT ).contains( "update" ) );
			assertThat( pizza.getLastUpdated() ).isAfter( updatedTime );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Pizza pizza = new Pizza( 1L, "Salamino" );
			session.persist( pizza );
			final Topping topping = new Topping();
			topping.setName( "Pepperoni" );
			topping.setPizza( pizza );
			pizza.getToppings().add( topping );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Pizza" )
	static class Pizza {
		@Id
		private Long id;

		@OneToMany( mappedBy = "pizza", cascade = CascadeType.ALL )
		private List<Topping> toppings = new ArrayList<>();

		@CurrentTimestamp
		private ZonedDateTime lastUpdated;

		private String name;

		public Pizza() {
		}

		public Pizza(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public List<Topping> getToppings() {
			return toppings;
		}

		public void setToppings(final List<Topping> toppings) {
			this.toppings = toppings;
		}

		public ZonedDateTime getLastUpdated() {
			return lastUpdated;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Topping" )
	static class Topping {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Pizza pizza;

		private String name;

		public void setName(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setPizza(final Pizza pizza) {
			this.pizza = pizza;
		}

	}
}
