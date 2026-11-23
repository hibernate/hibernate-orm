package org.hibernate.orm.test.collection.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link Hibernate#get(Map, Object)} of an uninitialized map keyed by entities
 * looks up the given key: not a value, when the values are entities of the same
 * type, and by the unique key the foreign key refers to, not by the identifier.
 */
@DomainModel(annotatedClasses = {
		EntityKeyAndValueLookupTest.Owner.class,
		EntityKeyAndValueLookupTest.Item.class,
		EntityKeyAndValueLookupTest.Catalog.class,
		EntityKeyAndValueLookupTest.CodedItem.class,
		EntityKeyAndValueLookupTest.Board.class,
		EntityKeyAndValueLookupTest.Tag.class,
		EntityKeyAndValueLookupTest.Shelf.class,
		EntityKeyAndValueLookupTest.Book.class,
		EntityKeyAndValueLookupTest.Region.class,
		EntityKeyAndValueLookupTest.Office.class,
		EntityKeyAndValueLookupTest.Branch.class,
		EntityKeyAndValueLookupTest.Bank.class
})
@SessionFactory
@JiraKey("HHH-21218")
public class EntityKeyAndValueLookupTest {

	@Test
	void getByEntityKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var key = new Item();
			key.id = 10;
			session.persist( key );
			final var value = new Item();
			value.id = 20;
			session.persist( value );
			final var owner = new Owner();
			owner.id = 1;
			owner.items.put( key, value );
			session.persist( owner );
		} );
		scope.inSession( session -> {
			final var owner = session.find( Owner.class, 1 );
			assertSame( session.find( Item.class, 20 ), Hibernate.get( owner.items, session.find( Item.class, 10 ) ) );
			// the value is not a key
			assertNull( Hibernate.get( owner.items, session.find( Item.class, 20 ) ) );
			assertFalse( Hibernate.isInitialized( owner.items ) );
		} );
	}

	@Test
	void getByEntityKeyReferringToUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// the identifier of each item is the code of the other
			final var first = new CodedItem( 10, 20 );
			final var second = new CodedItem( 20, 10 );
			session.persist( first );
			session.persist( second );
			final var catalog = new Catalog();
			catalog.id = 1;
			catalog.labels.put( first, "first" );
			catalog.labels.put( second, "second" );
			session.persist( catalog );
		} );
		scope.inSession( session -> {
			final var catalog = session.find( Catalog.class, 1 );
			assertEquals( "first", Hibernate.get( catalog.labels, session.find( CodedItem.class, 10 ) ) );
			assertEquals( "second", Hibernate.get( catalog.labels, session.find( CodedItem.class, 20 ) ) );
			assertFalse( Hibernate.isInitialized( catalog.labels ) );
		} );
	}

	@Test
	void getByUnsavedKeyWithUniqueKeyOfStoredKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var stored = new Tag();
			stored.code = "KEY";
			session.persist( stored );
			final var board = new Board();
			board.id = 1;
			board.labels.put( stored, "stored" );
			session.persist( board );
		} );
		scope.inSession( session -> {
			final var board = session.find( Board.class, 1 );
			final var unsaved = new Tag();
			unsaved.code = "KEY";
			// an unsaved entity is not a key of the map, whatever its unique key
			assertNull( Hibernate.get( board.labels, unsaved ) );
			assertFalse( Hibernate.isInitialized( board.labels ) );
		} );
	}

	@Test
	void getByDetachedProxyForKeyReferringToUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new CodedItem( 60, 61 );
			session.persist( item );
			final var catalog = new Catalog();
			catalog.id = 3;
			catalog.labels.put( item, "detached" );
			session.persist( catalog );
		} );
		final var detached = scope.fromSession( session -> session.getReference( CodedItem.class, 60 ) );
		assertFalse( Hibernate.isInitialized( detached ) );
		scope.inSession( session -> {
			final var labels = session.find( Catalog.class, 3 ).labels;
			// the unique key is read in this session, whether or not it holds the entity
			assertEquals( "detached", Hibernate.get( labels, detached ) );
			session.find( CodedItem.class, 60 );
			assertEquals( "detached", Hibernate.get( labels, detached ) );
			assertFalse( Hibernate.isInitialized( labels ) );
			assertFalse( Hibernate.isInitialized( detached ) );
		} );
	}

	@Test
	void getByDetachedKeyWithStaleUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new CodedItem( 70, 71 ) ) );
		final var detached = scope.fromSession( session -> session.find( CodedItem.class, 70 ) );
		scope.inTransaction( session -> {
			// the detached item still has the code which now belongs to another item
			final var original = session.find( CodedItem.class, 70 );
			original.code = 72;
			session.flush();
			final var other = new CodedItem( 80, 71 );
			session.persist( other );
			final var catalog = new Catalog();
			catalog.id = 4;
			catalog.labels.put( original, "original" );
			catalog.labels.put( other, "other" );
			session.persist( catalog );
		} );
		scope.inSession( session -> {
			final var labels = session.find( Catalog.class, 4 ).labels;
			assertEquals( "original", Hibernate.get( labels, detached ) );
			assertFalse( Hibernate.isInitialized( labels ) );
		} );
	}

	@Test
	void getByUnsavedKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shelf = new Shelf();
			shelf.id = 1;
			session.persist( shelf );
		} );
		scope.inSession( session -> {
			final var shelf = session.find( Shelf.class, 1 );
			assertNull( Hibernate.get( shelf.labels, new Book() ) );
			assertFalse( Hibernate.isInitialized( shelf.labels ) );
		} );
	}

	@Test
	void getByProxyForKeyReferringToCompositeUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var key = new Region();
			key.id = 10;
			key.code = "KEY";
			session.persist( key );
			final var other = new Region();
			other.id = 20;
			other.code = "VAL";
			session.persist( other );
			final var office = new Office();
			office.id = 1;
			office.labels.put( key, "member" );
			session.persist( office );
		} );
		scope.inSession( session -> {
			final var office = session.find( Office.class, 1 );
			final var key = session.getReference( Region.class, 10 );
			assertFalse( Hibernate.isInitialized( key ) );
			assertEquals( "member", Hibernate.get( office.labels, key ) );
			assertNull( Hibernate.get( office.labels, session.getReference( Region.class, 20 ) ) );
			assertFalse( Hibernate.isInitialized( office.labels ) );
			assertFalse( Hibernate.isInitialized( key ) );
		} );
	}

	@Test
	void getByKeyReferringToEmbeddedUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var key = new Branch();
			key.id = 10;
			key.uniqueCode = new BranchCode( "KEY" );
			session.persist( key );
			final var other = new Branch();
			other.id = 20;
			other.uniqueCode = new BranchCode( "VAL" );
			session.persist( other );
			final var bank = new Bank();
			bank.id = 1;
			session.persist( bank );
			session.flush();
			session.createNativeMutationQuery(
					"insert into Bank_labels (bank_id, key_code, key_region, labels) values (1, 'KEY', 'EU', 'member')" )
					.executeUpdate();
		} );
		scope.inSession( session -> {
			final var bank = session.find( Bank.class, 1 );
			assertEquals( "member", Hibernate.get( bank.labels, session.find( Branch.class, 10 ) ) );
			assertNull( Hibernate.get( bank.labels, session.find( Branch.class, 20 ) ) );
			assertFalse( Hibernate.isInitialized( bank.labels ) );
		} );
	}

	@Entity(name = "Shelf")
	static class Shelf {
		@Id
		int id;
		@ElementCollection
		@CollectionTable(name = "Shelf_labels")
		@MapKeyJoinColumn(name = "book_id")
		Map<Book, String> labels = new HashMap<>();
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "Region")
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"code", "region"}))
	static class Region implements Serializable {
		@Id
		int id;
		String code;
		String region = "EU";
	}

	@Entity(name = "Office")
	static class Office {
		@Id
		int id;
		@ElementCollection
		@CollectionTable(name = "Office_labels")
		@MapKeyJoinColumns({
				@MapKeyJoinColumn(name = "key_code", referencedColumnName = "code"),
				@MapKeyJoinColumn(name = "key_region", referencedColumnName = "region")
		})
		Map<Region, String> labels = new HashMap<>();
	}

	@Embeddable
	static class BranchCode implements Serializable {
		String code;
		String region;

		BranchCode() {
		}

		BranchCode(String code) {
			this.code = code;
			this.region = "EU";
		}
	}

	@Entity(name = "Branch")
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"code", "region"}))
	static class Branch implements Serializable {
		@Id
		int id;
		@Embedded
		BranchCode uniqueCode;
	}

	@Entity(name = "Bank")
	static class Bank {
		@Id
		int id;
		@ElementCollection
		@CollectionTable(name = "Bank_labels", joinColumns = @JoinColumn(name = "bank_id"))
		@MapKeyJoinColumns({
				@MapKeyJoinColumn(name = "key_code", referencedColumnName = "code"),
				@MapKeyJoinColumn(name = "key_region", referencedColumnName = "region")
		})
		Map<Branch, String> labels = new HashMap<>();
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		int id;
		@ManyToMany
		@MapKeyJoinColumn(name = "key_id")
		@JoinTable(name = "Owner_items", inverseJoinColumns = @JoinColumn(name = "val_id"))
		Map<Item, Item> items = new HashMap<>();
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		int id;
	}

	@Entity(name = "Catalog")
	static class Catalog {
		@Id
		int id;
		@ElementCollection
		@CollectionTable(name = "Catalog_labels")
		@MapKeyJoinColumn(name = "item_code", referencedColumnName = "code")
		Map<CodedItem, String> labels = new HashMap<>();
	}

	@Entity(name = "Board")
	static class Board {
		@Id
		int id;
		@ElementCollection
		@CollectionTable(name = "Board_labels")
		@MapKeyJoinColumn(name = "tag_code", referencedColumnName = "code")
		Map<Tag, String> labels = new HashMap<>();
	}

	@Entity(name = "Tag")
	static class Tag implements Serializable {
		@Id
		@GeneratedValue
		Integer id;
		@Column(unique = true)
		String code;
	}

	@Entity(name = "CodedItem")
	static class CodedItem implements Serializable {
		@Id
		int id;
		@Column(unique = true)
		int code;

		CodedItem() {
		}

		CodedItem(int id, int code) {
			this.id = id;
			this.code = code;
		}
	}
}
