/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JoinFetchInheritanceTest.Zoo.class,
		JoinFetchInheritanceTest.Animal.class,
		JoinFetchInheritanceTest.Cat.class,
		JoinFetchInheritanceTest.Kitten.class,
		JoinFetchInheritanceTest.CatEmbedded.class,
		JoinFetchInheritanceTest.RootEmbeddable.class,
		JoinFetchInheritanceTest.KittensEmbeddable.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16555" )
public class JoinFetchInheritanceTest {
	private final static String CAT = "cat";
	private final static String CAT_EMBEDDED = "cat_embedded";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Animal> animals = new ArrayList<>();
			final Kitten kitten1 = new Kitten( "kitten_1" );
			session.persist( kitten1 );
			animals.add( new Cat( 1L, List.of( kitten1 ), kitten1 ) );
			animals.add( new Cat( 2L, new ArrayList<>(), kitten1 ) );
			final Kitten kitten2 = new Kitten( "kitten_2" );
			session.persist( kitten2 );
			animals.add( new CatEmbedded( 3L, new KittensEmbeddable( List.of( kitten2 ), kitten2 ) ) );
			animals.add( new CatEmbedded( 4L, new KittensEmbeddable( new ArrayList<>(), kitten2 ) ) );
			animals.forEach( session::persist );
			session.persist( new Zoo( animals ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "from Cat", Cat.class )
					.getResultList()
					.forEach( JoinFetchInheritanceTest::clearKittens );
			session.createQuery( "from CatEmbedded", CatEmbedded.class )
					.getResultList()
					.forEach( JoinFetchInheritanceTest::clearKittens );
		} );
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Animal" ).executeUpdate();
			session.createMutationQuery( "delete from Kitten" ).executeUpdate();
			session.createMutationQuery( "delete from Zoo" ).executeUpdate();
		} );
	}

	@Test
	public void testCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animal from Animal animal " +
				"left join fetch animal.kittens",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, true, true );
	}

	@Test
	public void testPluralJoinCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animals from Zoo zoo join zoo.animals animals " +
				"left join fetch animals.kittens",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, true, true );
	}

	@Test
	public void testSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animal from Animal animal " +
				"left join fetch animal.singleKitten",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, false, true );
	}

	@Test
	public void testPluralJoinSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animals from Zoo zoo join zoo.animals animals " +
				"left join fetch animals.singleKitten",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, false, true );
	}

	@Test
	public void testEmbeddedCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animal from Animal animal " +
				"left join fetch animal.rootEmbeddable.kittensEmbeddable.kittens",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, true, false );
	}

	@Test
	public void testPluralJoinEmbeddedCollection(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animals from Zoo zoo join zoo.animals animals " +
				"left join fetch animals.rootEmbeddable.kittensEmbeddable.kittens",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, true, false );
	}

	@Test
	public void testEmbeddedSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animal from Animal animal " +
				"left join fetch animal.rootEmbeddable.kittensEmbeddable.singleKitten",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, false, false );
	}

	@Test
	public void testPluralJoinEmbeddedSingle(SessionFactoryScope scope) {
		final List<Animal> animals = scope.fromTransaction( session -> session.createQuery(
				"select animals from Zoo zoo join zoo.animals animals " +
				"left join fetch animals.rootEmbeddable.kittensEmbeddable.singleKitten",
				Animal.class
		).getResultList() );
		assertCatInitialized( animals, false, false );
	}

	/**
	 * Checks for correct association initialization.
	 *
	 * @param animals the animal collection, result of the query
	 * @param collection {@code true} check collection association, {@code false} check to-one association
	 * @param simpleInitialized {@code true} the simple (non-embedded) entity associations should be initialized,
	 * {@code false} the embedded type should be initialized
	 */
	private static void assertCatInitialized(
			List<Animal> animals,
			boolean collection,
			boolean simpleInitialized) {
		assertThat( animals ).hasSize( 4 );
		for ( final Animal animal : animals ) {
			final KittenContainer kittenContainer = (KittenContainer) animal;
			if ( collection ) {
				final List<Kitten> kittens = kittenContainer.getKittens();
				if ( simpleInitialized && animal.getType().equals( CAT ) ||
					!simpleInitialized && animal.getType().equals( CAT_EMBEDDED ) ) {
					assertThat( Hibernate.isInitialized( kittens ) ).isTrue();
					assertThat( kittens ).hasSizeBetween( 0, 1 );
				}
				else {
					assertThat( Hibernate.isInitialized( kittens ) ).isFalse();
				}
			}
			else {
				final Kitten kitten = kittenContainer.getSingleKitten();
				if ( simpleInitialized && animal.getType().equals( CAT ) ||
					!simpleInitialized && animal.getType().equals( CAT_EMBEDDED ) ) {
					assertThat( Hibernate.isInitialized( kitten ) ).isTrue();
					assertThat( kitten.getName() ).isEqualTo( simpleInitialized ? "kitten_1" : "kitten_2" );
				}
				else {
					assertThat( Hibernate.isInitialized( kitten ) ).isFalse();
				}
			}
		}
	}

	private static void clearKittens(KittenContainer container) {
		container.getKittens().clear();
	}

	@Entity( name = "Zoo" )
	public static class Zoo {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany
		@JoinColumn( name = "zoo_id" )
		private List<Animal> animals;

		public Zoo() {
		}

		public Zoo(List<Animal> animals) {
			this.animals = animals;
		}

		public List<Animal> getAnimals() {
			return animals;
		}
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "type", discriminatorType = DiscriminatorType.STRING )
	public static class Animal {
		@Id
		private Long id;

		public Animal() {
		}

		public Animal(Long id) {
			this.id = id;
		}

		@Column( name = "type", insertable = false, updatable = false )
		private String type;

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}
	}

	public interface KittenContainer {
		String getType();

		List<Kitten> getKittens();

		Kitten getSingleKitten();
	}

	@Entity( name = "Cat" )
	@DiscriminatorValue( CAT )
	public static class Cat extends Animal implements KittenContainer {
		@ManyToMany( fetch = FetchType.LAZY )
		@JoinTable( name = "cat_kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "kitten_id" )
		public Kitten singleKitten;

		public Cat() {
		}

		public Cat(Long id, List<Kitten> kittens, Kitten singleKitten) {
			super( id );
			this.kittens = kittens;
			this.singleKitten = singleKitten;
		}

		public List<Kitten> getKittens() {
			return kittens;
		}

		public Kitten getSingleKitten() {
			return singleKitten;
		}
	}

	@Embeddable
	public static class RootEmbeddable {
		@Embedded
		private KittensEmbeddable kittensEmbeddable;

		public RootEmbeddable() {
		}

		public RootEmbeddable(KittensEmbeddable kittensEmbeddable) {
			this.kittensEmbeddable = kittensEmbeddable;
		}

		public KittensEmbeddable getKittensEmbeddable() {
			return kittensEmbeddable;
		}
	}

	@Embeddable
	public static class KittensEmbeddable {
		@ManyToMany( fetch = FetchType.LAZY )
		@JoinTable( name = "cat_embedded_kitten" )
		public List<Kitten> kittens = new ArrayList<>();

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "embedded_kitten_id" )
		public Kitten singleKitten;

		public KittensEmbeddable() {
		}

		public KittensEmbeddable(List<Kitten> kittens, Kitten singleKitten) {
			this.kittens = kittens;
			this.singleKitten = singleKitten;
		}

		public List<Kitten> getKittens() {
			return kittens;
		}

		public Kitten getSingleKitten() {
			return singleKitten;
		}
	}

	@Entity( name = "CatEmbedded" )
	@DiscriminatorValue( CAT_EMBEDDED )
	public static class CatEmbedded extends Animal implements KittenContainer {
		@Embedded
		private RootEmbeddable rootEmbeddable;

		public CatEmbedded() {
		}

		public CatEmbedded(Long id, KittensEmbeddable kittensEmbeddable) {
			super( id );
			this.rootEmbeddable = new RootEmbeddable( kittensEmbeddable );
		}

		public List<Kitten> getKittens() {
			return rootEmbeddable.getKittensEmbeddable().getKittens();
		}

		public Kitten getSingleKitten() {
			return rootEmbeddable.getKittensEmbeddable().getSingleKitten();
		}
	}

	@Entity( name = "Kitten" )
	public static class Kitten {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Kitten() {
		}

		public Kitten(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
