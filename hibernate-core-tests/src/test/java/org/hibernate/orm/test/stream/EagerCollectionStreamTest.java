package org.hibernate.orm.test.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Eager collections must be available after consuming the first stream result and closing the entity manager.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14115" )
@Jpa( annotatedClasses = { EagerCollectionStreamTest.Basket.class, EagerCollectionStreamTest.Fruit.class } )
public class EagerCollectionStreamTest {
	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( long basketId = 1; basketId <= 2; basketId++ ) {
				final Basket basket = new Basket();
				basket.id = basketId;
				entityManager.persist( basket );
				for ( long fruitId : basketId == 1 ? new long[] { 1, 2 } : new long[] { 3 } ) {
					final Fruit fruit = new Fruit();
					fruit.id = fruitId;
					fruit.basket = basket;
					basket.fruits.add( fruit );
					entityManager.persist( fruit );
				}
			}
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testEagerCollectionAfterFindFirstAndClose(EntityManagerFactoryScope scope) {
		final Basket basket = scope.fromEntityManager( entityManager -> {
			try (Stream<Basket> stream = entityManager.createQuery(
					"select b from Basket b order by b.id", Basket.class
			).getResultStream()) {
				final Basket first = stream.findFirst().orElseThrow();
				assertTrue( Hibernate.isInitialized( first.fruits ) );
				return first;
			}
		} );

		assertEquals( 1L, basket.id );
		assertEquals( 2, basket.fruits.size() );
		assertEquals( List.of( 1L, 2L ), basket.fruits.stream().map( fruit -> fruit.id ).sorted().toList() );
		basket.fruits.forEach( fruit -> assertSame( basket, fruit.basket ) );
	}

	@Entity( name = "Basket" )
	@Table( name = "hhh14115_basket" )
	public static class Basket {
		@Id
		private Long id;

		@OneToMany( mappedBy = "basket", fetch = FetchType.EAGER )
		private List<Fruit> fruits = new ArrayList<>();
	}

	@Entity( name = "Fruit" )
	@Table( name = "hhh14115_fruit" )
	public static class Fruit {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private Basket basket;
	}
}
