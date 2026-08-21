/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests cross-type entity change tracking via a custom
 * {@link Changelog @Changelog} with
 * {@link Changelog.ModifiedEntities @Changelog.ModifiedEntities}.
 * <p>
 * Exercises the REVCHANGES write-side (via {@code @ElementCollection}
 * on the changeset entity) and the read-side APIs on {@code AuditLog}.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		RevChangesTrackingTest.Book.class,
		RevChangesTrackingTest.Author.class,
		RevChangesTrackingTest.TrackingRevisionInfo.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RevChangesTrackingTest {

	// --- Custom changeset entity with @Changelog.ModifiedEntities ---

	@Changelog
	@Entity(name = "TrackingRevisionInfo")
	@Table(name = "REVINFO")
	static class TrackingRevisionInfo {
		@Id
		@GeneratedValue
		@Changelog.ChangesetId
		@Column(name = "REV")
		int id;

		@Changelog.Timestamp
		@Column(name = "REVTSTMP")
		long timestamp;

		@ElementCollection(fetch = FetchType.EAGER)
		@JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
		@Column(name = "ENTITYNAME")
		@Fetch(FetchMode.JOIN)
		@Changelog.ModifiedEntities
		Set<String> modifiedEntityNames = new HashSet<>();
	}

	// --- Audited entities ---

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		long id;
		String title;
	}

	@Audited
	@Entity(name = "Author")
	static class Author {
		@Id
		long id;
		String name;
	}

	// --- Test data ---

	/**
	 * Rev 1: create Book + Author
	 */
	private Object rev1;
	/**
	 * Rev 2: update Book only
	 */
	private Object rev2;
	/**
	 * Rev 3: delete Author only
	 */
	private Object rev3;

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		// Rev 1: create both entities
		scope.getSessionFactory().inTransaction( session -> {
			final var book = new Book();
			book.id = 1L;
			book.title = "Hibernate in Action";
			session.persist( book );

			final var author = new Author();
			author.id = 1L;
			author.name = "Gavin King";
			session.persist( author );
		} );
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			rev1 = auditLog.getChangesets( Book.class, 1L ).get( 0 );
		}

		// Rev 2: update Book only
		scope.getSessionFactory().inTransaction( session ->
				session.find( Book.class, 1L ).title = "Hibernate in Action 2e"
		);
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( Book.class, 1L );
			rev2 = revisions.get( revisions.size() - 1 );
		}

		// Rev 3: delete Author only
		scope.getSessionFactory().inTransaction( session ->
				session.remove( session.find( Author.class, 1L ) )
		);
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( Author.class, 1L );
			rev3 = revisions.get( revisions.size() - 1 );
		}
	}

	// --- Schema verification ---

	@Test
	@Order(1)
	void testRevChangesTableExists(DomainModelScope scope) {
		boolean found = false;
		for ( var table : scope.getDomainModel().collectTableMappings() ) {
			if ( "REVCHANGES".equalsIgnoreCase( table.getName() ) ) {
				found = true;
				assertNotNull( table.getColumn( new org.hibernate.mapping.Column( "REV" ) ) );
				assertNotNull( table.getColumn( new org.hibernate.mapping.Column( "ENTITYNAME" ) ) );
				break;
			}
		}
		assertTrue( found, "REVCHANGES table not found in metadata" );
	}

	// --- getEntityTypesModifiedAt ---

	@Test
	@Order(2)
	void testEntityTypesModifiedAtMultiType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: both Book and Author were created
			final var types = auditLog.getEntityTypesModifiedAt( rev1 );
			assertEquals( 2, types.size() );
			assertTrue( types.contains( Book.class ) );
			assertTrue( types.contains( Author.class ) );
		}
	}

	@Test
	@Order(3)
	void testEntityTypesModifiedAtSingleType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 2: only Book was modified
			final var types = auditLog.getEntityTypesModifiedAt( rev2 );
			assertEquals( 1, types.size() );
			assertTrue( types.contains( Book.class ) );
		}
	}

	// --- findAllEntitiesModifiedAt ---

	@Test
	@Order(4)
	void testFindAllEntitiesModifiedAtMultiType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: both entities created
			final var entities = auditLog.findAllEntitiesModifiedAt( rev1 );
			assertEquals( 2, entities.size() );
		}
	}

	@Test
	@Order(5)
	void testFindAllEntitiesModifiedAtSingleType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 2: only Book updated
			final var entities = auditLog.findAllEntitiesModifiedAt( rev2 );
			assertEquals( 1, entities.size() );
			assertTrue( entities.get( 0 ) instanceof Book );
			assertEquals( "Hibernate in Action 2e", ((Book) entities.get( 0 )).title );
		}
	}

	// --- findAllEntitiesModifiedAt with ModificationType ---

	@Test
	@Order(6)
	void testFindAllEntitiesModifiedAtWithAddFilter(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: both were ADDs
			final var adds = auditLog.findAllEntitiesModifiedAt( rev1, ModificationType.ADD );
			assertEquals( 2, adds.size() );

			// No MODs or DELs in rev 1
			assertTrue( auditLog.findAllEntitiesModifiedAt( rev1, ModificationType.MOD ).isEmpty() );
			assertTrue( auditLog.findAllEntitiesModifiedAt( rev1, ModificationType.DEL ).isEmpty() );
		}
	}

	@Test
	@Order(7)
	void testFindAllEntitiesModifiedAtWithDelFilter(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 3: Author was deleted
			final var dels = auditLog.findAllEntitiesModifiedAt( rev3, ModificationType.DEL );
			assertEquals( 1, dels.size() );
		}
	}

	// --- findAllEntitiesGroupedByModificationType ---

	@Test
	@Order(8)
	void testGroupedByModificationTypeAllAdds(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: both entities were ADDs
			final var grouped = auditLog.findAllEntitiesGroupedByModificationType( rev1 );
			assertEquals( 2, grouped.get( ModificationType.ADD ).size() );
			assertTrue( grouped.get( ModificationType.MOD ).isEmpty() );
			assertTrue( grouped.get( ModificationType.DEL ).isEmpty() );
		}
	}

	@Test
	@Order(9)
	void testGroupedByModificationTypeMod(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 2: Book was MOD
			final var grouped = auditLog.findAllEntitiesGroupedByModificationType( rev2 );
			assertTrue( grouped.get( ModificationType.ADD ).isEmpty() );
			assertEquals( 1, grouped.get( ModificationType.MOD ).size() );
			assertTrue( grouped.get( ModificationType.MOD ).get( 0 ) instanceof Book );
			assertTrue( grouped.get( ModificationType.DEL ).isEmpty() );
		}
	}

	@Test
	@Order(10)
	void testGroupedByModificationTypeDel(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 3: Author was DEL
			final var grouped = auditLog.findAllEntitiesGroupedByModificationType( rev3 );
			assertTrue( grouped.get( ModificationType.ADD ).isEmpty() );
			assertTrue( grouped.get( ModificationType.MOD ).isEmpty() );
			assertEquals( 1, grouped.get( ModificationType.DEL ).size() );
		}
	}

	// --- Revision entity verification ---

	@Test
	@Order(11)
	void testChangelogHasModifiedEntityNames(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Load the changeset entity and verify modifiedEntityNames was populated
			TrackingRevisionInfo revInfo = auditLog.findChangeset( TrackingRevisionInfo.class, rev1 );
			assertNotNull( revInfo.modifiedEntityNames );
			assertEquals( 2, revInfo.modifiedEntityNames.size() );
			// Entity names are FQN of the @Entity name mapping
			assertTrue( revInfo.modifiedEntityNames.stream()
					.anyMatch( n -> n.contains( "Book" ) ) );
			assertTrue( revInfo.modifiedEntityNames.stream()
					.anyMatch( n -> n.contains( "Author" ) ) );
		}
	}
}
