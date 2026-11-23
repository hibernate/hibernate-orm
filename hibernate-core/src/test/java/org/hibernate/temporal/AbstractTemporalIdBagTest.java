package org.hibernate.temporal;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.Temporal;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;

/**
 * Replacing an element of a temporal idbag ends the version of the row which held the replaced
 * element and starts a new version of that row, with the same collection id. The collection id of
 * {@code owner_tags} is a {@code Long}, and that of {@code owner_labels} an {@code Integer}, which
 * is easily mistaken for the position of an element.
 */
@JiraKey("HHH-21243")
abstract class AbstractTemporalIdBagTest {
	private static final String TAGS = "owner_tags";
	private static final String LABELS = "owner_labels";

	/**
	 * The table which holds every version of the rows of the given collection table.
	 */
	abstract String versionsTable(String collectionTable);

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(strings = { TAGS, LABELS })
	void testSetElement(String table, SessionFactoryScope scope) {
		final var change = change( scope, table, elements -> elements.set( 0, "x" ) );

		assertReplaced( scope, table, change );
	}

	/**
	 * An idbag gives the row of a removed element to the element added after it.
	 */
	@ParameterizedTest
	@ValueSource(strings = { TAGS, LABELS })
	void testRemoveAndAddElement(String table, SessionFactoryScope scope) {
		final var change = change( scope, table, elements -> {
			final String removed = elements.remove( 0 );
			elements.add( "x" );
			return removed;
		} );

		assertReplaced( scope, table, change );
	}

	@ParameterizedTest
	@ValueSource(strings = { TAGS, LABELS })
	void testRemoveElement(String table, SessionFactoryScope scope) {
		final var change = change( scope, table, elements -> elements.remove( 0 ) );

		final var current = new TreeMap<>( change.versionsBefore() );
		current.remove( change.rowId() );
		assertThat( versions( scope, table, true ) ).isEqualTo( current );
		assertThat( versions( scope, table, false ) ).isEqualTo( Map.of( change.rowId(), change.element() ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { TAGS, LABELS })
	void testAddElement(String table, SessionFactoryScope scope) {
		final var change = change( scope, table, elements -> {
			elements.add( "x" );
			return null;
		} );

		final var current = versions( scope, table, true );
		assertThat( current ).hasSize( 4 ).containsAllEntriesOf( change.versionsBefore() );
		current.keySet().removeAll( change.versionsBefore().keySet() );
		assertThat( current.values() ).containsExactly( "x" );
		assertThat( versions( scope, table, false ) ).isEmpty();
	}

	/**
	 * The current version of the row which held the replaced element now holds {@code x}, the ended
	 * version of that row still holds the replaced element, and no other row has a new version.
	 */
	private void assertReplaced(SessionFactoryScope scope, String table, Change change) {
		final var current = new TreeMap<>( change.versionsBefore() );
		current.put( change.rowId(), "x" );
		assertThat( versions( scope, table, true ) ).isEqualTo( current );
		assertThat( versions( scope, table, false ) ).isEqualTo( Map.of( change.rowId(), change.element() ) );
		scope.inTransaction( session -> assertThat( elements( session.find( Owner.class, 1L ), table ) )
				.containsExactlyInAnyOrderElementsOf( current.values() ) );
	}

	/**
	 * Applies a change to a collection loaded in a later transaction than the one which wrote it. The
	 * change returns the element it replaced or removed.
	 */
	private Change change(SessionFactoryScope scope, String table, Function<List<String>, String> change) {
		scope.inTransaction( session -> {
			final Owner owner = new Owner();
			owner.tags.addAll( List.of( "a", "b", "c" ) );
			owner.labels.addAll( List.of( "a", "b", "c" ) );
			session.persist( owner );
		} );
		awaitTimestampTick();
		final var versionsBefore = versions( scope, table, true );
		assertThat( versionsBefore.values() ).containsExactlyInAnyOrder( "a", "b", "c" );
		final String element = scope.fromTransaction(
				session -> change.apply( elements( session.find( Owner.class, 1L ), table ) ) );
		awaitTimestampTick();
		return new Change( versionsBefore, element );
	}

	private static List<String> elements(Owner owner, String table) {
		return TAGS.equals( table ) ? owner.tags : owner.labels;
	}

	/**
	 * The current or the ended versions of the rows of a collection table, as the element by
	 * collection id.
	 */
	private SortedMap<Long, String> versions(SessionFactoryScope scope, String table, boolean current) {
		final String sql = "select id, elem from " + versionsTable( table )
				+ " where owner_id = 1 and superseded is " + ( current ? "null" : "not null" );
		final SortedMap<Long, String> versions = new TreeMap<>();
		scope.inTransaction( session -> {
			for ( Object[] row : session.createNativeQuery( sql, Object[].class ).getResultList() ) {
				// the Java type of an integer column read by a native query depends on the database
				assertThat( versions.put( ( (Number) row[0] ).longValue(), (String) row[1] ) ).isNull();
			}
		} );
		return versions;
	}

	private record Change(SortedMap<Long, String> versionsBefore, String element) {
		/**
		 * The collection id of the row which held the replaced or removed element.
		 */
		Long rowId() {
			return versionsBefore.entrySet().stream()
					.filter( row -> Objects.equals( row.getValue(), element ) )
					.map( Map.Entry::getKey )
					.findFirst().orElseThrow();
		}
	}

	@Temporal
	@Entity(name = "Owner")
	// each starts from 1, so that the collection id of an element is the position of the next element
	@SequenceGenerator(name = "owner_tags_seq", sequenceName = "owner_tags_seq", initialValue = 1)
	@SequenceGenerator(name = "owner_labels_seq", sequenceName = "owner_labels_seq", initialValue = 1)
	static class Owner {
		@Id
		Long id = 1L;

		@Temporal
		@ElementCollection
		@CollectionTable(name = TAGS, joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "elem")
		@CollectionId(column = @Column(name = "id"), generator = "owner_tags_seq")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		List<String> tags = new ArrayList<>();

		@Temporal
		@ElementCollection
		@CollectionTable(name = LABELS, joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "elem")
		@CollectionId(column = @Column(name = "id"), generator = "owner_labels_seq")
		@CollectionIdJdbcTypeCode(Types.INTEGER)
		List<String> labels = new ArrayList<>();
	}
}
