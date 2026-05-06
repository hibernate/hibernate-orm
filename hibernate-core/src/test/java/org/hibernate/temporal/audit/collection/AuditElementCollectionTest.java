/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.annotations.SortNatural;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests @Audited element collections: indexed lists, maps, embeddable sets.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditElementCollectionTest.ListEntity.class,
		AuditElementCollectionTest.MapEntity.class,
		AuditElementCollectionTest.EmbeddableSetEntity.class,
		AuditElementCollectionTest.ArrayEntity.class,
		AuditElementCollectionTest.SortedSetEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditElementCollectionTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditElementCollectionTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// List scenario (IDs 1-9)
	private int revListCreate; // ListEntity(1) with [alpha, beta]
	private int revListMod;    // add gamma, remove alpha

	// Map scenario (IDs 1-9, separate entity type)
	private int revMapCreate;  // MapEntity(1) with {key1=value1, key2=value2}
	private int revMapMod;     // update key1, add key3, remove key2

	// Embeddable set scenario (IDs 1-9, separate entity type)
	private int revEmbCreate;  // EmbeddableSetEntity(1) with {Alice/90, Bob/85}
	private int revEmbMod;     // remove Alice, add Charlie/95

	// Array scenario (IDs 1-9, separate entity type)
	private int revArrCreate;  // ArrayEntity(1) with [alpha, beta]
	private int revArrMod;     // replace to [gamma, beta, delta]

	// SortedSet scenario (IDs 1-9, separate entity type)
	private int revSsCreate;   // SortedSetEntity(1) with {alpha, beta}
	private int revSsMod;      // remove alpha, add gamma

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- List with @OrderColumn ---

		sf.inTransaction( session -> {
			var e = new ListEntity( 1L );
			e.strings.add( "alpha" );
			e.strings.add( "beta" );
			session.persist( e );
		} );
		revListCreate = currentTxId;

		sf.inTransaction( session -> {
			var e = session.find( ListEntity.class, 1L );
			e.strings.add( "gamma" );
			e.strings.remove( 0 );
		} );
		revListMod = currentTxId;

		// --- Map<String, String> ---

		sf.inTransaction( session -> {
			var e = new MapEntity( 1L );
			e.strings.put( "key1", "value1" );
			e.strings.put( "key2", "value2" );
			session.persist( e );
		} );
		revMapCreate = currentTxId;

		sf.inTransaction( session -> {
			var e = session.find( MapEntity.class, 1L );
			e.strings.put( "key1", "updated1" );
			e.strings.put( "key3", "value3" );
			e.strings.remove( "key2" );
		} );
		revMapMod = currentTxId;

		// --- Set<Embeddable> ---

		sf.inTransaction( session -> {
			var e = new EmbeddableSetEntity( 1L );
			e.components.add( new Component( "Alice", 90 ) );
			e.components.add( new Component( "Bob", 85 ) );
			session.persist( e );
		} );
		revEmbCreate = currentTxId;

		sf.inTransaction( session -> {
			var e = session.find( EmbeddableSetEntity.class, 1L );
			e.components.removeIf( c -> c.name.equals( "Alice" ) );
			e.components.add( new Component( "Charlie", 95 ) );
		} );
		revEmbMod = currentTxId;

		// --- String array with @OrderColumn ---

		sf.inTransaction( session -> {
			var e = new ArrayEntity( 1L );
			e.strings = new String[] {"alpha", "beta"};
			session.persist( e );
		} );
		revArrCreate = currentTxId;

		sf.inTransaction( session -> {
			var e = session.find( ArrayEntity.class, 1L );
			e.strings = new String[] {"gamma", "beta", "delta"};
		} );
		revArrMod = currentTxId;

		// --- SortedSet<String> with @SortNatural ---

		sf.inTransaction( session -> {
			var e = new SortedSetEntity( 1L );
			e.tags.add( "beta" );
			e.tags.add( "alpha" );
			session.persist( e );
		} );
		revSsCreate = currentTxId;

		sf.inTransaction( session -> {
			var e = session.find( SortedSetEntity.class, 1L );
			e.tags.remove( "alpha" );
			e.tags.add( "gamma" );
		} );
		revSsMod = currentTxId;
	}

	// ---- List with @OrderColumn ----

	@Test
	@Order(1)
	void testIndexedList(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getRevisions( ListEntity.class, 1L ) ).hasSize( 2 );
		}

		// At revListCreate: [alpha, beta]
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revListCreate ).openSession()) {
			var e = s.find( ListEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsExactly( "alpha", "beta" );
		}

		// At revListMod: [beta, gamma] (alpha removed, gamma added)
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revListMod ).openSession()) {
			var e = s.find( ListEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsExactly( "beta", "gamma" );
		}

		// Verify DEL audit rows store both index and element value
		scope.inSession( session -> {
			var delRows = session.createNativeQuery(
					"select strings, strings_ORDER from ListEntity_strings_AUD"
					+ " where REVTYPE = 2 order by strings_ORDER", Tuple.class
			).getResultList();
			assertThat( delRows ).hasSizeGreaterThanOrEqualTo( 1 );
			assertThat( delRows ).anySatisfy( row -> {
				assertThat( row.get( "strings" ) ).isEqualTo( "alpha" );
				assertThat( row.get( "strings_ORDER" ) ).isEqualTo( 0 );
			} );
		} );
	}

	// ---- Map<String, String> ----

	@Test
	@Order(2)
	void testStringMap(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getRevisions( MapEntity.class, 1L ) ).hasSize( 2 );
		}

		// At revMapCreate: {key1=value1, key2=value2}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revMapCreate ).openSession()) {
			var e = s.find( MapEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsEntry( "key1", "value1" )
					.containsEntry( "key2", "value2" )
					.hasSize( 2 );
		}

		// At revMapMod: {key1=updated1, key3=value3}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revMapMod ).openSession()) {
			var e = s.find( MapEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsEntry( "key1", "updated1" )
					.containsEntry( "key3", "value3" )
					.hasSize( 2 );
		}

		// Verify DEL audit rows store both key and value
		scope.inSession( session -> {
			var delRows = session.createNativeQuery(
					"select strings_KEY, strings from MapEntity_strings_AUD"
					+ " where REVTYPE = 2 order by strings_KEY", Tuple.class
			).getResultList();
			// key1 value update -> DEL old + ADD new
			assertThat( delRows ).anySatisfy( row -> {
				assertThat( row.get( "strings_KEY" ) ).isEqualTo( "key1" );
				assertThat( row.get( "strings" ) ).isEqualTo( "value1" );
			} );
			// key2 removal -> DEL with full entry
			assertThat( delRows ).anySatisfy( row -> {
				assertThat( row.get( "strings_KEY" ) ).isEqualTo( "key2" );
				assertThat( row.get( "strings" ) ).isEqualTo( "value2" );
			} );
		} );
	}

	// ---- Set<Embeddable> ----

	@Test
	@Order(3)
	void testEmbeddableSet(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getRevisions( EmbeddableSetEntity.class, 1L ) ).hasSize( 2 );
		}

		// At revEmbCreate: {Alice/90, Bob/85}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revEmbCreate ).openSession()) {
			var e = s.find( EmbeddableSetEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.components ).extracting( c -> c.name )
					.containsExactlyInAnyOrder( "Alice", "Bob" );
		}

		// At revEmbMod: {Bob/85, Charlie/95}
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revEmbMod ).openSession()) {
			var e = s.find( EmbeddableSetEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.components ).extracting( c -> c.name )
					.containsExactlyInAnyOrder( "Bob", "Charlie" );
		}

		// Verify DEL audit rows store the full embeddable (name + score)
		scope.inSession( session -> {
			var delRows = session.createNativeQuery(
					"select name_col as name, score from EmbeddableSetEntity_components_AUD"
					+ " where REVTYPE = 2", Tuple.class
			).getResultList();
			assertThat( delRows ).hasSize( 1 );
			assertThat( delRows.get( 0 ).get( "name" ) ).isEqualTo( "Alice" );
			assertThat( delRows.get( 0 ).get( "score" ) ).isEqualTo( 90 );
		} );
	}

	// ---- String array with @OrderColumn ----

	@Test
	@Order(4)
	void testStringArray(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getRevisions( ArrayEntity.class, 1L ) ).hasSize( 2 );
		}

		// At revArrCreate: [alpha, beta]
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revArrCreate ).openSession()) {
			var e = s.find( ArrayEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsExactly( "alpha", "beta" );
		}

		// At revArrMod: [gamma, beta, delta]
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revArrMod ).openSession()) {
			var e = s.find( ArrayEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.strings ).containsExactly( "gamma", "beta", "delta" );
		}

		// Verify diff: "beta" unchanged at index 1, no audit rows for it in revArrMod.
		scope.inSession( session -> {
			var rev2Rows = session.createNativeQuery(
					"select strings, strings_ORDER, REVTYPE from ArrayEntity_strings_AUD"
					+ " where REV = " + revArrMod + " order by strings_ORDER, REVTYPE", Tuple.class
			).getResultList();
			assertThat( rev2Rows ).noneMatch( row -> "beta".equals( row.get( "strings" ) ) );
		} );
	}

	// ---- SortedSet<String> with @SortNatural ----

	@Test
	@Order(5)
	void testSortedSet(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( auditLog.getRevisions( SortedSetEntity.class, 1L ) ).hasSize( 2 );
		}

		// At revSsCreate: {alpha, beta} (sorted)
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revSsCreate ).openSession()) {
			var e = s.find( SortedSetEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.tags ).containsExactly( "alpha", "beta" );
		}

		// At revSsMod: {beta, gamma} (sorted)
		try (var s = scope.getSessionFactory().withOptions().atChangeset( revSsMod ).openSession()) {
			var e = s.find( SortedSetEntity.class, 1L );
			assertThat( e ).isNotNull();
			assertThat( e.tags ).containsExactly( "beta", "gamma" );
		}
	}

	// --- ALL_REVISIONS collection isolation ---

	@Test
	@Order(6)
	void testCollectionRevisionIsolation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();
		try (var s = sf.withOptions().atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			var entities = s.createSelectionQuery( "from ArrayEntity where id = :id", ArrayEntity.class )
					.setParameter( "id", 1L )
					.getResultList();
			// revArrCreate([alpha,beta] size 2) + revArrMod([gamma,beta,delta] size 3) = 2 revisions
			assertEquals( 2, entities.size(), "Expected 2 revisions" );

			// Find revisions with different collection sizes
			ArrayEntity entityWith3 = null;
			ArrayEntity entityWith2 = null;
			for ( var e : entities ) {
				int size = e.strings.length;
				if ( size == 3 && entityWith3 == null ) {
					entityWith3 = e;
				}
				else if ( size == 2 && entityWith2 == null ) {
					entityWith2 = e;
				}
			}
			assertNotNull( entityWith3, "Should find a revision with 3 elements" );
			assertNotNull( entityWith2, "Should find a revision with 2 elements" );

			// Collections must be distinct instances across revisions
			assertNotSame( entityWith2.strings, entityWith3.strings,
					"Collections at different revisions must not be the same instance" );

			// Verify contents
			assertEquals( 3, entityWith3.strings.length );
			assertEquals( 2, entityWith2.strings.length );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "ListEntity")
	static class ListEntity {
		@Id
		long id;
		@ElementCollection
		@OrderColumn
		List<String> strings = new ArrayList<>();

		ListEntity() {
		}

		ListEntity(long id) {
			this.id = id;
		}
	}

	@Audited
	@Entity(name = "MapEntity")
	static class MapEntity {
		@Id
		long id;
		@ElementCollection
		@MapKeyColumn(nullable = false)
		Map<String, String> strings = new HashMap<>();

		MapEntity() {
		}

		MapEntity(long id) {
			this.id = id;
		}
	}

	@Audited
	@Entity(name = "EmbeddableSetEntity")
	static class EmbeddableSetEntity {
		@Id
		long id;
		@ElementCollection
		Set<Component> components = new HashSet<>();

		EmbeddableSetEntity() {
		}

		EmbeddableSetEntity(long id) {
			this.id = id;
		}
	}

	@Audited
	@Entity(name = "ArrayEntity")
	static class ArrayEntity {
		@Id
		long id;
		@ElementCollection
		@OrderColumn
		String[] strings;

		ArrayEntity() {
		}

		ArrayEntity(long id) {
			this.id = id;
		}
	}

	@Audited
	@Entity(name = "SortedSetEntity")
	static class SortedSetEntity {
		@Id
		long id;
		@ElementCollection
		@SortNatural
		SortedSet<String> tags = new TreeSet<>();

		SortedSetEntity() {
		}

		SortedSetEntity(long id) {
			this.id = id;
		}
	}

	@Embeddable
	static class Component {
		@Column(name = "name_col")
		String name;
		int score;

		Component() {
		}

		Component(String name, int score) {
			this.name = name;
			this.score = score;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Component c && Objects.equals( name, c.name ) && score == c.score;
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, score );
		}
	}
}
