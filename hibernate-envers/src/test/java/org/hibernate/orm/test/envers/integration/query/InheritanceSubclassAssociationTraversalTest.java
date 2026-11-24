package org.hibernate.orm.test.envers.integration.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.envers.query.AuditEntity.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Traversing an association whose declared target type is a super class and then traversing an
 * association which is only declared on one of its sub classes.
 * <p>
 * {@code Shelf.item} is typed with the abstract {@code Item}, {@code publisher} is only declared on
 * the sub class {@code Book}. The equivalent HQL
 * {@code select s from Shelf s join treat(s.item as Book) b join b.publisher p} works.
 */
@JiraKey(value = "HHH-21251")
@Jpa(annotatedClasses = {
		InheritanceSubclassAssociationTraversalTest.Shelf.class,
		InheritanceSubclassAssociationTraversalTest.Item.class,
		InheritanceSubclassAssociationTraversalTest.Book.class,
		InheritanceSubclassAssociationTraversalTest.Novel.class,
		InheritanceSubclassAssociationTraversalTest.Dvd.class,
		InheritanceSubclassAssociationTraversalTest.Game.class,
		InheritanceSubclassAssociationTraversalTest.Publisher.class,
		InheritanceSubclassAssociationTraversalTest.Rack.class,
		InheritanceSubclassAssociationTraversalTest.Media.class,
		InheritanceSubclassAssociationTraversalTest.Magazine.class,
		InheritanceSubclassAssociationTraversalTest.Podcast.class
})
@EnversTest
public class InheritanceSubclassAssociationTraversalTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			final Publisher acme = new Publisher( 1, "Acme" );
			final Publisher other = new Publisher( 2, "Other" );
			em.persist( acme );
			em.persist( other );

			final Book acmeBook = new Book( 1, acme, new Edition( "First" ) );
			acmeBook.getDistributors().add( other );
			acmeBook.getTranslations().add( new Edition( "German" ) );
			final Book otherBook = new Book( 2, other, new Edition( "Second" ) );
			final Dvd dvd = new Dvd( 3, acme );
			final Novel acmeNovel = new Novel( 4, acme, new Edition( "First" ) );
			final Game game = new Game( 5, other );
			em.persist( acmeBook );
			em.persist( otherBook );
			em.persist( dvd );
			em.persist( acmeNovel );
			em.persist( game );

			em.persist( new Shelf( 1, acmeBook ) );
			em.persist( new Shelf( 2, otherBook ) );
			em.persist( new Shelf( 3, dvd ) );
			em.persist( new Shelf( 4, acmeNovel ) );
			em.persist( new Shelf( 5, game ) );

			final Magazine acmeMagazine = new Magazine( 1, acme );
			final Podcast podcast = new Podcast( 2 );
			em.persist( acmeMagazine );
			em.persist( podcast );
			em.persist( new Rack( 1, acmeMagazine ) );
			em.persist( new Rack( 2, podcast ) );
		} );
	}

	@Test
	public void testTraverseToOneDeclaredOnSubclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Shelf.class, 1 )
					.traverseRelation( "item", JoinType.INNER, "i" )
					.traverseRelation( "publisher", JoinType.INNER, "p" )
					.add( property( "p", "name" ).eq( "Acme" ) )
					.up()
					.up();
			// Shelf 4 holds a Novel, which inherits publisher from Book
			assertEquals( Set.of( 1, 4 ), shelfIds( query.getResultList() ) );
		} );
	}

	@Test
	public void testTraverseToManyDeclaredOnSubclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Shelf.class, 1 )
					.traverseRelation( "item", JoinType.INNER, "i" )
					.traverseRelation( "distributors", JoinType.INNER, "d" )
					.add( property( "d", "name" ).eq( "Other" ) )
					.up()
					.up();
			assertEquals( Set.of( 1 ), shelfIds( query.getResultList() ) );
		} );
	}

	@Test
	public void testTraverseComponentDeclaredOnSubclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Shelf.class, 1 )
					.traverseRelation( "item", JoinType.INNER, "i" )
					.traverseRelation( "edition", JoinType.INNER, "e" )
					.add( property( "e", "label" ).eq( "First" ) )
					.up()
					.up();
			assertEquals( Set.of( 1, 4 ), shelfIds( query.getResultList() ) );
		} );
	}

	@Test
	public void testTraverseComponentCollectionDeclaredOnSubclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final AuditQuery query = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Shelf.class, 1 )
					.traverseRelation( "item", JoinType.INNER, "i" )
					.traverseRelation( "translations", JoinType.INNER, "t" )
					.add( property( "t", "label" ).eq( "German" ) )
					.up()
					.up();
			assertEquals( Set.of( 1 ), shelfIds( query.getResultList() ) );
		} );
	}

	@Test
	public void testTraverseToOneDeclaredOnSingleTableSubclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final List<?> results = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Rack.class, 1 )
					.traverseRelation( "media", JoinType.INNER, "m" )
					.traverseRelation( "publisher", JoinType.INNER, "p" )
					.add( property( "p", "name" ).eq( "Acme" ) )
					.up()
					.up()
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1, ( (Rack) results.get( 0 ) ).getId() );
		} );
	}

	@Test
	public void testTraverseAssociationDeclaredOnMultipleSubclasses(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// studio is declared on Dvd and on Game, so it is ambiguous via Item
			final IllegalArgumentException e = assertThrows(
					IllegalArgumentException.class,
					() -> AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( Shelf.class, 1 )
							.traverseRelation( "item", JoinType.INNER, "i" )
							.traverseRelation( "studio", JoinType.INNER, "s" )
			);
			assertTrue( e.getMessage().contains( "declared on multiple sub entities" ), e.getMessage() );
		} );
	}

	@Test
	public void testTraverseUnknownAssociation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final IllegalArgumentException e = assertThrows(
					IllegalArgumentException.class,
					() -> AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( Shelf.class, 1 )
							.traverseRelation( "item", JoinType.INNER, "i" )
							.traverseRelation( "unknown", JoinType.INNER, "u" )
			);
			assertTrue( e.getMessage().contains( "is not a valid association for queries" ), e.getMessage() );
		} );
	}

	private static Set<Integer> shelfIds(List<?> shelves) {
		final Set<Integer> ids = new TreeSet<>();
		for ( Object shelf : shelves ) {
			ids.add( ( (Shelf) shelf ).getId() );
		}
		return ids;
	}

	@Entity(name = "Shelf")
	@Audited
	public static class Shelf {
		@Id
		private Integer id;
		@ManyToOne
		private Item item;

		Shelf() {
		}

		Shelf(Integer id, Item item) {
			this.id = id;
			this.item = item;
		}

		public Integer getId() {
			return id;
		}

		public Item getItem() {
			return item;
		}
	}

	@Entity(name = "Item")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class Item {
		@Id
		private Integer id;

		Item() {
		}

		Item(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "Book")
	@Audited
	public static class Book extends Item {
		@ManyToOne
		private Publisher publisher;
		@ManyToMany
		private Set<Publisher> distributors = new HashSet<>();
		@Embedded
		private Edition edition;
		@ElementCollection
		private Set<Edition> translations = new HashSet<>();

		Book() {
		}

		Book(Integer id, Publisher publisher, Edition edition) {
			super( id );
			this.publisher = publisher;
			this.edition = edition;
		}

		public Publisher getPublisher() {
			return publisher;
		}

		public Set<Publisher> getDistributors() {
			return distributors;
		}

		public Edition getEdition() {
			return edition;
		}

		public Set<Edition> getTranslations() {
			return translations;
		}
	}

	@Entity(name = "Novel")
	@Audited
	public static class Novel extends Book {
		Novel() {
		}

		Novel(Integer id, Publisher publisher, Edition edition) {
			super( id, publisher, edition );
		}
	}

	@Entity(name = "Dvd")
	@Audited
	public static class Dvd extends Item {
		@ManyToOne
		private Publisher studio;

		Dvd() {
		}

		Dvd(Integer id, Publisher studio) {
			super( id );
			this.studio = studio;
		}

		public Publisher getStudio() {
			return studio;
		}
	}

	@Entity(name = "Game")
	@Audited
	public static class Game extends Item {
		@ManyToOne
		private Publisher studio;

		Game() {
		}

		Game(Integer id, Publisher studio) {
			super( id );
			this.studio = studio;
		}

		public Publisher getStudio() {
			return studio;
		}
	}

	@Embeddable
	public static class Edition {
		private String label;

		Edition() {
		}

		Edition(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Edition other && java.util.Objects.equals( label, other.label );
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hashCode( label );
		}
	}

	@Entity(name = "Publisher")
	@Audited
	public static class Publisher {
		@Id
		private Integer id;
		private String name;

		Publisher() {
		}

		Publisher(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Rack")
	@Audited
	public static class Rack {
		@Id
		private Integer id;
		@ManyToOne
		private Media media;

		Rack() {
		}

		Rack(Integer id, Media media) {
			this.id = id;
			this.media = media;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "Media")
	@Audited
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract static class Media {
		@Id
		private Integer id;

		Media() {
		}

		Media(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "Magazine")
	@Audited
	public static class Magazine extends Media {
		@ManyToOne
		private Publisher publisher;

		Magazine() {
		}

		Magazine(Integer id, Publisher publisher) {
			super( id );
			this.publisher = publisher;
		}

		public Publisher getPublisher() {
			return publisher;
		}
	}

	@Entity(name = "Podcast")
	@Audited
	public static class Podcast extends Media {
		Podcast() {
		}

		Podcast(Integer id) {
			super( id );
		}
	}
}
