/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gavin King
 */
@DomainModel( annotatedClasses = {
		HqlDetachedCollectionSelectionTest.Author.class,
		HqlDetachedCollectionSelectionTest.Book.class,
		HqlDetachedCollectionSelectionTest.CatalogItem.class,
		HqlDetachedCollectionSelectionTest.CatalogArchive.class,
		HqlDetachedCollectionSelectionTest.Shelf.class,
} )
@SessionFactory
@JiraKey( "HHH-17558" )
public class HqlDetachedCollectionSelectionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author gavin = new Author( 1, "Gavin King" );
			final Author steve = new Author( 2, "Steve Ebersole" );
			session.persist( gavin );
			session.persist( steve );

			final Book book = new Book( 1, "Hibernate ORM" );
			book.getAuthors().add( gavin );
			book.getAuthors().add( steve );
			book.getTags().add( "hql" );
			book.getTags().add( "orm" );
			book.getChapters().add( "Introduction" );
			book.getChapters().add( "Query Language" );
			book.getTranslations().put( "de", "Hibernate ORM auf Deutsch" );
			book.getTranslations().put( "fr", "Hibernate ORM en francais" );
			session.persist( book );

			final CatalogItem first = new CatalogItem( 1, "First pick" );
			final CatalogItem second = new CatalogItem( 2, "Second pick" );
			final CatalogWithItems catalog = new CatalogWithItems( "Recommended" );
			catalog.getItems().add( first );
			catalog.getItems().add( second );
			session.persist( new Shelf( 1, catalog ) );

			final CatalogArchive catalogArchive = new CatalogArchive( 1 );
			catalogArchive.getCatalogs().add( new Catalog( "Seasonal" ) );
			catalogArchive.getCatalogs().add( new Catalog( "Clearance" ) );
			session.persist( catalogArchive );

			final CatalogArchive otherCatalogArchive = new CatalogArchive( 2 );
			otherCatalogArchive.getCatalogs().add( new Catalog( "Reference" ) );
			otherCatalogArchive.getCatalogs().add( new Catalog( "Backlist" ) );
			session.persist( otherCatalogArchive );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPluralAttributeSelectionReturnsDetachedCollection(SessionFactoryScope scope) {
		final Collection<Author> authors =
				scope.fromTransaction(
						session -> session.createSelectionQuery( "select book.authors from Hhh17558Book book",
								collectionType( Author.class ) ).getSingleResult()
				);

		assertDetachedSet( authors );
		assertThat( authors )
				.extracting( Author::getName )
				.containsExactlyInAnyOrder( "Gavin King", "Steve Ebersole" );
	}

	@Test
	public void testMapPluralAttributeSelectionReturnsDetachedMap(SessionFactoryScope scope) {
		final Map<String, String> translations =
				scope.fromTransaction(
						session -> session.createSelectionQuery(
								"select book.translations from Hhh17558Book book",
								mapType( String.class, String.class )
						).getSingleResult()
				);

		assertDetachedMap( translations );
		assertThat( translations )
				.containsEntry( "de", "Hibernate ORM auf Deutsch" )
				.containsEntry( "fr", "Hibernate ORM en francais" )
				.hasSize( 2 );
	}

	@Test
	public void testElementsFunctionReturnsDetachedCollection(SessionFactoryScope scope) {
		final Collection<Author> authors =
				scope.fromTransaction(
						session1 -> session1.createSelectionQuery(
								"select elements(book.authors) from Hhh17558Book book",
								collectionType( Author.class ) ).getSingleResult()
				);
		final Collection<String> tags =
				scope.fromTransaction(
						session -> session.createSelectionQuery( "select elements(book.tags) from Hhh17558Book book",
								collectionType( String.class ) ).getSingleResult()
				);

		assertDetachedSet( authors );
		assertThat( authors )
				.extracting( Author::getName )
				.containsExactlyInAnyOrder( "Gavin King", "Steve Ebersole" );
		assertDetachedSet( tags );
		assertThat( tags ).containsExactlyInAnyOrder( "hql", "orm" );
	}

	@Test
	public void testElementFunctionStillFlattensCollection(SessionFactoryScope scope) {
		final Collection<String> authorNames = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select element(book.authors).name from Hhh17558Book book",
						String.class
				).getResultList()
		);
		final Collection<String> tags = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select element(book.tags) from Hhh17558Book book",
						String.class
				).getResultList()
		);

		assertThat( authorNames ).containsExactlyInAnyOrder( "Gavin King", "Steve Ebersole" );
		assertThat( tags ).containsExactlyInAnyOrder( "hql", "orm" );
	}

	@Test
	public void testKeysAndValuesFunctionsReturnDetachedCollections(SessionFactoryScope scope) {
		final Collection<String> locales =
				scope.fromTransaction(
						session1 -> session1.createSelectionQuery(
								"select keys(book.translations) from Hhh17558Book book",
								collectionType( String.class ) ).getSingleResult()
				);
		final Collection<String> translations =
				scope.fromTransaction(
						session -> session.createSelectionQuery(
								"select values(book.translations) from Hhh17558Book book",
								collectionType( String.class ) ).getSingleResult()
				);

		assertDetachedSet( locales );
		assertThat( locales ).containsExactlyInAnyOrder( "de", "fr" );
		assertDetachedSet( translations );
		assertThat( translations ).containsExactlyInAnyOrder(
				"Hibernate ORM auf Deutsch",
				"Hibernate ORM en francais"
		);
	}

	@Test
	public void testIndicesFunctionReturnsListIndexes(SessionFactoryScope scope) {
		final Collection<Integer> indexes =
				scope.fromTransaction(
						session -> session.createSelectionQuery(
								"select indices(book.chapters) from Hhh17558Book book",
								collectionType( Integer.class )
						).getSingleResult()
				);

		assertDetachedSet( indexes );
		assertThat( indexes ).containsExactly( 0, 1 );
	}

	@Test
	public void testKeyAndValueFunctionsStillFlattenMap(SessionFactoryScope scope) {
		final Collection<String> locales = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select key(book.translations) from Hhh17558Book book",
						String.class
				).getResultList()
		);
		final Collection<String> translations = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select value(book.translations) from Hhh17558Book book",
						String.class
				).getResultList()
		);

		assertThat( locales ).containsExactlyInAnyOrder( "de", "fr" );
		assertThat( translations ).containsExactlyInAnyOrder(
				"Hibernate ORM auf Deutsch",
				"Hibernate ORM en francais"
		);
	}

	@Test
	public void testEmbeddableSelectionReturnsDetachedCollection(SessionFactoryScope scope) {
		final CatalogWithItems catalog = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select shelf.catalog from Hhh17558Shelf shelf",
						CatalogWithItems.class
				).getSingleResult()
		);

		assertCatalogWithItems( catalog );
	}

	@Test
	public void testUntypedEmbeddableSelectionReturnsDetachedCollection(SessionFactoryScope scope) {
		final Object result = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select shelf.catalog from Hhh17558Shelf shelf",
						Object.class
				).getSingleResult()
		);

		assertThat( result ).isInstanceOf( CatalogWithItems.class );
		assertCatalogWithItems( (CatalogWithItems) result );
	}

	@Test
	public void testTupleEmbeddableSelectionReturnsDetachedCollection(SessionFactoryScope scope) {
		final Object[] result = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select shelf.id, shelf.catalog from Hhh17558Shelf shelf",
						Object[].class
				).getSingleResult()
		);

		assertThat( result[0] ).isEqualTo( 1 );
		assertCatalogWithItems( (CatalogWithItems) result[1] );
	}

	@Test
	public void testElementCollectionOfCatalogsSelectionReturnsDetachedCollection(SessionFactoryScope scope) {
		final Collection<Catalog> catalogs = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select archive.catalogs from Hhh17558CatalogArchive archive where archive.id = 1",
						collectionType( Catalog.class )
				).getSingleResult()
		);

		assertDetachedList( catalogs );
		assertThat( catalogs )
				.extracting( Catalog::getName )
				.containsExactly( "Seasonal", "Clearance" );
	}

	@Test
	public void testElementCollectionOfCatalogsSelectionReturnsMultipleDetachedCollections(SessionFactoryScope scope) {
		final List<Collection<Catalog>> catalogLists = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select archive.catalogs from Hhh17558CatalogArchive archive order by archive.id",
						collectionType( Catalog.class )
				).getResultList()
		);

		assertThat( catalogLists ).hasSize( 2 );
		assertDetachedList( catalogLists.get( 0 ) );
		assertThat( catalogLists.get( 0 ) )
				.extracting( Catalog::getName )
				.containsExactly( "Seasonal", "Clearance" );
		assertDetachedList( catalogLists.get( 1 ) );
		assertThat( catalogLists.get( 1 ) )
				.extracting( Catalog::getName )
				.containsExactly( "Reference", "Backlist" );
	}

	private static void assertCatalogWithItems(CatalogWithItems catalog) {
		assertThat( catalog.getName() ).isEqualTo( "Recommended" );
		assertDetachedSet( catalog.getItems() );
		assertThat( catalog.getItems() )
				.extracting( CatalogItem::getName )
				.containsExactlyInAnyOrder( "First pick", "Second pick" );
	}

	@SuppressWarnings("unchecked")
	private static @NonNull <T> Class<Collection<T>> collectionType(Class<T> elementType) {
		return (Class<Collection<T>>) (Class) Collection.class;
	}

	@SuppressWarnings("unchecked")
	private static @NonNull <K, V> Class<Map<K, V>> mapType(Class<K> keyType, Class<V> valueType) {
		return (Class<Map<K, V>>) (Class) Map.class;
	}

	private static void assertDetachedSet(Collection<?> collection) {
		assertThat( collection ).isInstanceOf( Set.class );
		assertThat( collection ).isNotInstanceOf( PersistentCollection.class );
	}

	private static void assertDetachedList(Collection<?> collection) {
		assertThat( collection ).isInstanceOf( List.class );
		assertThat( collection ).isNotInstanceOf( PersistentCollection.class );
	}

	private static void assertDetachedMap(Map<?, ?> map) {
		assertThat( map ).isInstanceOf( Map.class );
		assertThat( map ).isNotInstanceOf( PersistentCollection.class );
	}

	@Entity( name = "Hhh17558Author" )
	@Table( name = "hhh17558_author" )
	public static class Author {
		@Id
		private Integer id;

		private String name;

		protected Author() {
		}

		private Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Hhh17558Book" )
	@Table( name = "hhh17558_book" )
	public static class Book {
		@Id
		private Integer id;

		private String title;

		@ManyToMany
		@JoinTable( name = "hhh17558_book_author",
				joinColumns = @JoinColumn( name = "book_id" ),
				inverseJoinColumns = @JoinColumn( name = "author_id" ) )
		private Set<Author> authors = new HashSet<>();

		@ElementCollection
		@CollectionTable( name = "hhh17558_book_tag", joinColumns = @JoinColumn( name = "book_id" ) )
		@Column( name = "tag" )
		private Set<String> tags = new HashSet<>();

		@ElementCollection
		@CollectionTable( name = "hhh17558_book_chapter", joinColumns = @JoinColumn( name = "book_id" ) )
		@OrderColumn( name = "chapter_index" )
		@Column( name = "chapter" )
		private List<String> chapters = new ArrayList<>();

		@ElementCollection
		@CollectionTable( name = "hhh17558_book_translation", joinColumns = @JoinColumn( name = "book_id" ) )
		@MapKeyColumn( name = "locale" )
		@Column( name = "translation" )
		private Map<String, String> translations = new HashMap<>();

		protected Book() {
		}

		private Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}

		public Set<Author> getAuthors() {
			return authors;
		}

		public Set<String> getTags() {
			return tags;
		}

		public List<String> getChapters() {
			return chapters;
		}

		public Map<String, String> getTranslations() {
			return translations;
		}
	}

	@Entity( name = "Hhh17558Shelf" )
	@Table( name = "hhh17558_shelf" )
	public static class Shelf {
		@Id
		private Integer id;

		@Embedded
		private CatalogWithItems catalog;

		protected Shelf() {
		}

		private Shelf(Integer id, CatalogWithItems catalog) {
			this.id = id;
			this.catalog = catalog;
		}
	}

	@Embeddable
	public static class CatalogWithItems {
		private String name;

		@OneToMany( cascade = CascadeType.PERSIST, fetch = FetchType.LAZY )
		@JoinColumn( name = "shelf_id" )
		private Set<CatalogItem> items = new HashSet<>();

		protected CatalogWithItems() {
		}

		private CatalogWithItems(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<CatalogItem> getItems() {
			return items;
		}
	}

	@Embeddable
	public static class Catalog {
		private String name;

		protected Catalog() {
		}

		private Catalog(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Hhh17558CatalogArchive" )
	@Table( name = "hhh17558_catalog_archive" )
	public static class CatalogArchive {
		@Id
		private Integer id;

		@ElementCollection
		@CollectionTable( name = "hhh17558_catalog_archive_catalog", joinColumns = @JoinColumn( name = "archive_id" ) )
		@OrderColumn( name = "catalog_index" )
		private List<Catalog> catalogs = new ArrayList<>();

		protected CatalogArchive() {
		}

		private CatalogArchive(Integer id) {
			this.id = id;
		}

		public List<Catalog> getCatalogs() {
			return catalogs;
		}
	}

	@Entity( name = "Hhh17558CatalogItem" )
	@Table( name = "hhh17558_catalog_item" )
	public static class CatalogItem {
		@Id
		private Integer id;

		private String name;

		protected CatalogItem() {
		}

		private CatalogItem(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
