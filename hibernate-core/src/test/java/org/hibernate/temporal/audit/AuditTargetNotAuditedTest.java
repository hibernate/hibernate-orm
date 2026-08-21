/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests @Audited entities with associations to non-audited entities.
 * <p>
 * Unlike the old envers module which required explicit {@code @Audited(targetAuditMode = NOT_AUDITED)},
 * core auditing handles this implicitly: the FK is stored in the audit table,
 * and the non-audited target is loaded from the current table.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditTargetNotAuditedTest.Product.class,
		AuditTargetNotAuditedTest.Category.class,
		AuditTargetNotAuditedTest.Tag.class,
		AuditTargetNotAuditedTest.Store.class,
		AuditTargetNotAuditedTest.Item.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditTargetNotAuditedTest$TxIdSupplier"))
class AuditTargetNotAuditedTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void setupTestData(SessionFactoryScope scope) {
		currentTxId = 0;

		// REV 1: create category and product with @ManyToOne association
		scope.getSessionFactory().inTransaction( session -> {
			var cat = new Category( 1L, "Electronics" );
			session.persist( cat );
			session.persist( new Product( 1L, "Phone", cat ) );
		} );

		// REV 2: change product's category
		scope.getSessionFactory().inTransaction( session -> {
			var cat2 = new Category( 2L, "Gadgets" );
			session.persist( cat2 );
			session.find( Product.class, 1L ).category = cat2;
		} );

		// REV 3: update the non-audited category name directly
		// (this does NOT create a product audit row)
		scope.getSessionFactory().inTransaction( session ->
				session.find( Category.class, 2L ).name = "Updated Gadgets"
		);

		// REV 4: clear the association
		scope.getSessionFactory().inTransaction( session ->
				session.find( Product.class, 1L ).category = null
		);
	}

	@AfterAll
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	// ---- @ManyToOne to non-audited ----

	@Test
	void testManyToOneWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( 3, auditLog.getChangesets( Product.class, 1L ).size() );
		}
	}

	@Test
	void testManyToOnePointInTimeRead(SessionFactoryScope scope) {
		// REV 1: product with Electronics category (FK=1)
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 1 ).openSession()) {
			var product = s.find( Product.class, 1L );
			assertNotNull( product );
			assertEquals( "Phone", product.name );
			assertNotNull( product.category );
			// FK from audit row is 1 -> loads Category#1
			assertEquals( 1L, product.category.id );
			assertEquals( "Electronics", product.category.name );
		}

		// REV 2: product switched to Gadgets (FK=2)
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 2 ).openSession()) {
			var product = s.find( Product.class, 1L );
			assertNotNull( product );
			assertNotNull( product.category );
			// FK from audit row is 2 -> loads Category#2
			assertEquals( 2L, product.category.id );
			// Non-audited target loads from current table,
			// so name reflects the rev 3 update
			assertEquals( "Updated Gadgets", product.category.name );
		}

		// REV 4: category cleared (FK=null)
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 4 ).openSession()) {
			var product = s.find( Product.class, 1L );
			assertNotNull( product );
			assertNull( product.category );
		}
	}

	@Test
	void testManyToOneDeletedTarget(SessionFactoryScope scope) {
		currentTxId = 400;

		scope.getSessionFactory().inTransaction( session -> {
			var cat = new Category( 99L, "Ephemeral" );
			session.persist( cat );
			session.persist( new Product( 99L, "Doomed", cat ) );
		} );

		// Switch to a different category so the FK is no longer 99
		scope.getSessionFactory().inTransaction( session -> {
			var cat2 = new Category( 98L, "Replacement" );
			session.persist( cat2 );
			session.find( Product.class, 99L ).category = cat2;
		} );

		// Delete the original non-audited category
		scope.getSessionFactory().inTransaction( session ->
				session.remove( session.find( Category.class, 99L ) )
		);

		// REV 1 pointed to Category#99 which no longer exists.
		// Loading at rev 1: the audit row has FK=99, but Category#99
		// has been deleted from the current table. Since @ManyToOne is
		// eagerly fetched via JOIN, the LEFT JOIN yields null columns
		// and the association resolves to null.
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 401 ).openSession()) {
			var product = s.find( Product.class, 99L );
			assertNotNull( product );
			assertEquals( "Doomed", product.name );
			assertNull( product.category,
					"Dangling FK to deleted non-audited entity should resolve to null" );
		}

		// REV 2 pointed to Category#98 which still exists
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 402 ).openSession()) {
			var product = s.find( Product.class, 99L );
			assertNotNull( product );
			assertNotNull( product.category );
			assertEquals( 98L, product.category.id );
		}
	}

	@Test
	void testManyToOneGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Product.class, 1L );
			assertEquals( 3, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertNotNull( history.get( 0 ).entity().category );

			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertNotNull( history.get( 1 ).entity().category );

			assertEquals( ModificationType.MOD, history.get( 2 ).modificationType() );
			assertNull( history.get( 2 ).entity().category );
		}
	}

	@Test
	void testManyToOneJoinFetchAllRevisions(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			final var rows = session.createSelectionQuery(
					"select e, changesetId(e), modificationType(e)"
					+ " from Product e left join fetch e.category"
					+ " where e.id = :id"
					+ " order by changesetId(e)",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 3, rows.size() );
			assertNotNull( ((Product) rows.get( 0 )[0]).category );
			assertNotNull( ((Product) rows.get( 1 )[0]).category );
			assertNull( ((Product) rows.get( 2 )[0]).category );
		}
	}

	// ---- @OneToOne to non-audited ----

	@Test
	void testOneToOneToNonAudited(SessionFactoryScope scope) {
		currentTxId = 100;

		scope.getSessionFactory().inTransaction( session -> {
			var store = new Store( 1L, "Main Store" );
			session.persist( store );
			var item = new Item( 1L, "Widget" );
			item.store = store;
			session.persist( item );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var store2 = new Store( 2L, "Branch" );
			session.persist( store2 );
			session.find( Item.class, 1L ).store = store2;
		} );

		// Point-in-time: non-audited store loads from current table
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 101 ).openSession()) {
			var item = s.find( Item.class, 1L );
			assertNotNull( item.store );
			assertEquals( "Main Store", item.store.name );
		}

		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 102 ).openSession()) {
			var item = s.find( Item.class, 1L );
			assertNotNull( item.store );
			assertEquals( "Branch", item.store.name );
		}

		// History
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Item.class, 1L );
			assertEquals( 2, history.size() );
			assertNotNull( history.get( 0 ).entity().store );
			assertNotNull( history.get( 1 ).entity().store );
		}
	}

	// ---- @ManyToMany to non-audited ----

	@Test
	void testManyToManyToNonAudited(SessionFactoryScope scope) {
		currentTxId = 200;

		scope.getSessionFactory().inTransaction( session -> {
			var tag1 = new Tag( 1L, "Sale" );
			var tag2 = new Tag( 2L, "New" );
			session.persist( tag1 );
			session.persist( tag2 );
			var product = new Product( 10L, "Laptop", null );
			product.tags.add( tag1 );
			session.persist( product );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var product = session.find( Product.class, 10L );
			product.tags.add( session.find( Tag.class, 2L ) );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var product = session.find( Product.class, 10L );
			product.tags.removeIf( t -> t.id == 1L );
		} );

		// Verify audit rows for the entity
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( 3, auditLog.getChangesets( Product.class, 10L ).size() );
		}

		// Point-in-time: tags should be loaded from current table
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 202 ).openSession()) {
			var product = s.find( Product.class, 10L );
			assertNotNull( product );
			assertEquals( "Laptop", product.name );
		}
	}

	// ---- @OneToMany to non-audited ----

	@Test
	void testOneToManyToNonAudited(SessionFactoryScope scope) {
		currentTxId = 300;

		scope.getSessionFactory().inTransaction( session -> {
			var cat = new Category( 10L, "Furniture" );
			session.persist( cat );
			var product = new Product( 20L, "Table", cat );
			product.relatedCategories.add( cat );
			session.persist( product );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var cat2 = new Category( 11L, "Home" );
			session.persist( cat2 );
			session.find( Product.class, 20L ).relatedCategories.add( cat2 );
		} );

		// Verify audit rows for the entity
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( 2, auditLog.getChangesets( Product.class, 20L ).size() );
		}

		// Point-in-time
		try (var s = scope.getSessionFactory().withOptions()
				.atChangeset( 301 ).openSession()) {
			var product = s.find( Product.class, 20L );
			assertNotNull( product );
			assertEquals( "Table", product.name );
		}
	}

	// ---- Entities ----

	@Audited
	@Entity(name = "Product")
	static class Product {
		@Id
		long id;
		String name;
		@ManyToOne
		Category category;
		@ManyToMany
		@JoinTable(name = "product_tags")
		Set<Tag> tags = new HashSet<>();
		@OneToMany
		@JoinColumn(name = "product_id")
		List<Category> relatedCategories = new ArrayList<>();

		Product() {
		}

		Product(long id, String name, Category category) {
			this.id = id;
			this.name = name;
			this.category = category;
		}
	}

	@Entity(name = "Category")
	static class Category {
		@Id
		long id;
		String name;

		Category() {
		}

		Category(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Tag")
	static class Tag {
		@Id
		long id;
		String name;

		Tag() {
		}

		Tag(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Item")
	static class Item {
		@Id
		long id;
		String name;
		@OneToOne
		Store store;

		Item() {
		}

		Item(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Store")
	static class Store {
		@Id
		long id;
		String name;

		Store() {
		}

		Store(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
