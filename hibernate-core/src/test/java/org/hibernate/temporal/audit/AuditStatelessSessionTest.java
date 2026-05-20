/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.audit.ModificationType;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises the full {@link Audited @Audited} lifecycle using only
 * {@link org.hibernate.StatelessSession} for both writes
 * ({@code insert}, {@code update}, {@code upsert}, {@code delete},
 * {@code insertMultiple}, {@code deleteMultiple}) and reads
 * (point-in-time via {@code atChangeset}, history retrieval),
 * combined with a custom {@link Changelog @Changelog} entity.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditStatelessSessionTest.Item.class,
		AuditStatelessSessionTest.Category.class,
		AuditStatelessSessionTest.RevInfo.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditStatelessSessionTest {

	@Changelog(listener = RevInfoListener.class)
	@Entity(name = "RevInfo")
	@Table(name = "REVINFO")
	static class RevInfo {
		@Id
		@GeneratedValue
		@Changelog.ChangesetId
		@Column(name = "REV")
		int id;

		@Changelog.Timestamp
		@Column(name = "REVTSTMP")
		Instant timestamp = Instant.now();

		@Column(name = "USERNAME")
		String username;
	}

	public static class RevInfoListener implements ChangesetListener {
		@Override
		public void newChangeset(Object changesetEntity) {
			((RevInfo) changesetEntity).username = "test-user";
		}
	}

	@Audited
	@Entity(name = "Category")
	static class Category {
		@Id
		long id;
		String label;

		Category() {
		}

		Category(long id, String label) {
			this.id = id;
			this.label = label;
		}
	}

	@Audited
	@Entity(name = "Item")
	static class Item {
		@Id
		long id;
		@Column(name = "item_name")
		String name;
		@ManyToOne
		Category category;

		Item() {
		}

		Item(long id, String name, Category category) {
			this.id = id;
			this.name = name;
			this.category = category;
		}
	}

	private Object revInsert;
	private Object revUpdate;
	private Object revUpsertUpdate;
	private Object revUpsertInsert;
	private Object revDelete;

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		// Changeset: insert Category + Item via stateless insert()
		scope.inStatelessTransaction( session -> {
			var cat = new Category( 1L, "Electronics" );
			session.insert( cat );
			session.insert( new Item( 1L, "Phone", cat ) );
		} );

		// Changeset: update Item name via stateless update()
		scope.inStatelessTransaction( session -> {
			var item = session.get( Item.class, 1L );
			item.name = "Smartphone";
			session.update( item );
		} );

		// Changeset: insert new Category + upsert Item to reassign category
		scope.inStatelessTransaction( session -> {
			var books = new Category( 2L, "Books" );
			session.insert( books );
			var item = session.get( Item.class, 1L );
			item.category = books;
			session.upsert( item );
		} );

		// Changeset: upsert as insert (new Item id=2)
		scope.inStatelessTransaction( session -> {
			var cat = session.get( Category.class, 1L );
			session.upsert( new Item( 2L, "Tablet", cat ) );
		} );

		// Changeset: delete Item id=1
		scope.inStatelessTransaction( session -> {
			var item = session.get( Item.class, 1L );
			session.delete( item );
		} );

		// Capture changeset IDs from the audit log
		try (var s = scope.getSessionFactory().openStatelessSession();
			var auditLog = AuditLogFactory.create( s )) {
			var item1Changesets = auditLog.getChangesets( Item.class, 1L );
			assertEquals( 4, item1Changesets.size() );
			revInsert = item1Changesets.get( 0 );
			revUpdate = item1Changesets.get( 1 );
			revUpsertUpdate = item1Changesets.get( 2 );
			revDelete = item1Changesets.get( 3 );

			var item2Changesets = auditLog.getChangesets( Item.class, 2L );
			assertEquals( 1, item2Changesets.size() );
			revUpsertInsert = item2Changesets.get( 0 );
		}
	}

	@Test
	@Order(1)
	void testInsertAuditTrail(SessionFactoryScope scope) {
		try (var s = scope.getSessionFactory().openStatelessSession();
			var auditLog = AuditLogFactory.create( s )) {
			assertEquals( ModificationType.ADD, auditLog.getModificationType( Item.class, 1L, revInsert ) );
			assertEquals( ModificationType.MOD, auditLog.getModificationType( Item.class, 1L, revUpdate ) );
			assertEquals( ModificationType.MOD, auditLog.getModificationType( Item.class, 1L, revUpsertUpdate ) );
			assertEquals( ModificationType.DEL, auditLog.getModificationType( Item.class, 1L, revDelete ) );
		}
	}

	@Test
	@Order(2)
	void testPointInTimeReads(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revInsert ).openStatelessSession()) {
			var item = s.get( Item.class, 1L );
			assertNotNull( item );
			assertEquals( "Phone", item.name );
			assertEquals( "Electronics", item.category.label );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revUpdate ).openStatelessSession()) {
			var item = s.get( Item.class, 1L );
			assertEquals( "Smartphone", item.name );
			assertEquals( "Electronics", item.category.label );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revUpsertUpdate ).openStatelessSession()) {
			var item = s.get( Item.class, 1L );
			assertEquals( "Smartphone", item.name );
			assertEquals( "Books", item.category.label );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revDelete ).openStatelessSession()) {
			assertNull( s.find( Item.class, 1L ) );
		}
	}

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var s = scope.getSessionFactory().openStatelessSession();
			var auditLog = AuditLogFactory.create( s )) {
			var history = auditLog.getHistory( Item.class, 1L );
			assertEquals( 4, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "Phone", history.get( 0 ).entity().name );

			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "Smartphone", history.get( 1 ).entity().name );

			assertEquals( ModificationType.MOD, history.get( 2 ).modificationType() );
			assertEquals( "Books", history.get( 2 ).entity().category.label );

			assertEquals( ModificationType.DEL, history.get( 3 ).modificationType() );
		}
	}

	@Test
	@Order(4)
	void testUpsertAsInsert(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.openStatelessSession();
			var auditLog = AuditLogFactory.create( s )) {
			var changesets = auditLog.getChangesets( Item.class, 2L );
			assertEquals( 1, changesets.size() );
			assertEquals( ModificationType.ADD, auditLog.getModificationType( Item.class, 2L, changesets.get( 0 ) ) );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revUpsertInsert ).openStatelessSession()) {
			var item = s.get( Item.class, 2L );
			assertNotNull( item );
			assertEquals( "Tablet", item.name );
			assertEquals( "Electronics", item.category.label );
		}

		// Not visible before its creation changeset
		try (var s = sf.withStatelessOptions().atChangeset( revInsert ).openStatelessSession()) {
			assertNull( s.find( Item.class, 2L ) );
		}
	}

	@Test
	@Order(5)
	void testAssociationNavigation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revInsert ).openStatelessSession()) {
			var item = s.get( Item.class, 1L );
			assertNotNull( item.category );
			assertEquals( 1L, item.category.id );
			assertEquals( "Electronics", item.category.label );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revUpsertUpdate ).openStatelessSession()) {
			var item = s.get( Item.class, 1L );
			assertNotNull( item.category );
			assertEquals( 2L, item.category.id );
			assertEquals( "Books", item.category.label );
		}
	}

	@Test
	@Order(6)
	void testDeletedEntityInvisible(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revDelete ).openStatelessSession()) {
			assertNull( s.find( Item.class, 1L ) );
		}

		// Still visible at the last modification before deletion
		try (var s = sf.withStatelessOptions().atChangeset( revUpsertUpdate ).openStatelessSession()) {
			assertNotNull( s.find( Item.class, 1L ) );
		}
	}

	@Test
	@Order(7)
	void testChangelogPopulated(SessionFactoryScope scope) {
		try (var s = scope.getSessionFactory().openStatelessSession();
			var auditLog = AuditLogFactory.create( s )) {
			var changesets = auditLog.getChangesets( Item.class, 1L );
			for ( var changesetId : changesets ) {
				var revInfo = auditLog.findChangeset( RevInfo.class, changesetId );
				assertNotNull( revInfo );
				assertEquals( "test-user", revInfo.username );
				assertNotNull( revInfo.timestamp );
			}
		}
	}

	@Test
	@Order(8)
	void testHqlQueryAtChangeset(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revUpdate ).openStatelessSession()) {
			var result = s.createSelectionQuery(
					"from Item where name = :n", Item.class
			).setParameter( "n", "Smartphone" ).getSingleResultOrNull();
			assertNotNull( result );
			assertEquals( 1L, result.id );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revInsert ).openStatelessSession()) {
			var result = s.createSelectionQuery(
					"from Item where name = :n", Item.class
			).setParameter( "n", "Smartphone" ).getSingleResultOrNull();
			assertNull( result );
		}
	}

	@Test
	@Order(9)
	void testBatchOperations(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Batch insert via insertMultiple
		scope.inStatelessTransaction( session -> {
			var cat = session.get( Category.class, 1L );
			session.insertMultiple( List.of(
					new Item( 100L, "Batch-A", cat ),
					new Item( 101L, "Batch-B", cat ),
					new Item( 102L, "Batch-C", cat )
			) );
		} );

		try (var ss = sf.openStatelessSession();
			var auditLog = AuditLogFactory.create( ss )) {
			assertEquals( 1, auditLog.getChangesets( Item.class, 100L ).size() );
			assertEquals( 1, auditLog.getChangesets( Item.class, 101L ).size() );
			assertEquals( 1, auditLog.getChangesets( Item.class, 102L ).size() );

			var revBatchInsert = auditLog.getChangesets( Item.class, 100L ).get( 0 );
			assertEquals( ModificationType.ADD, auditLog.getModificationType( Item.class, 100L, revBatchInsert ) );

			try (var s = sf.withStatelessOptions().atChangeset( revBatchInsert ).openStatelessSession()) {
				assertEquals( "Batch-A", s.get( Item.class, 100L ).name );
				assertEquals( "Batch-B", s.get( Item.class, 101L ).name );
				assertEquals( "Batch-C", s.get( Item.class, 102L ).name );
			}
		}

		// Batch delete via deleteMultiple
		scope.inStatelessTransaction( session -> {
			session.deleteMultiple( List.of(
					session.get( Item.class, 100L ),
					session.get( Item.class, 101L ),
					session.get( Item.class, 102L )
			) );
		} );

		try (var ss = sf.openStatelessSession();
			var auditLog = AuditLogFactory.create( ss )) {
			var revs100 = auditLog.getChangesets( Item.class, 100L );
			assertEquals( 2, revs100.size() );
			var revBatchDelete = revs100.get( 1 );
			assertEquals( ModificationType.DEL, auditLog.getModificationType( Item.class, 100L, revBatchDelete ) );

			try (var s = sf.withStatelessOptions().atChangeset( revBatchDelete ).openStatelessSession()) {
				assertNull( s.find( Item.class, 100L ) );
				assertNull( s.find( Item.class, 101L ) );
				assertNull( s.find( Item.class, 102L ) );
			}
		}
	}

	@Test
	@Order(10)
	void testReadOnlyStatelessDoesNotCreateChangeset(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		final long baselineCount;
		try (var s = sf.openStatelessSession()) {
			s.beginTransaction();
			baselineCount = s.createSelectionQuery(
					"select count(*) from RevInfo", Long.class
			).getSingleResult();
			s.getTransaction().commit();
		}

		// Read-only operations via stateless session
		try (var s = sf.withStatelessOptions().atChangeset( revInsert ).openStatelessSession()) {
			s.get( Item.class, 1L );
		}
		scope.inStatelessTransaction( session ->
				session.get( Item.class, 2L )
		);

		final long afterCount;
		try (var s = sf.openStatelessSession()) {
			s.beginTransaction();
			afterCount = s.createSelectionQuery(
					"select count(*) from RevInfo", Long.class
			).getSingleResult();
			s.getTransaction().commit();
		}

		assertEquals( baselineCount, afterCount, "Read-only stateless operations must not create changeset rows" );
	}
}
