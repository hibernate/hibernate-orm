package org.hibernate.orm.test.collection.idbag;

import java.util.TreeMap;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replacing an element of an idbag updates the row which held the replaced element, and leaves every
 * other row of the collection table alone. The tests read the rows back with a native query, since the
 * in-memory list is right whichever row was written.
 */
@JiraKey("HHH-21243")
public abstract class AbstractIdBagElementReplacementTest extends IdBagFixture {

	@ParameterizedTest(name = CASES)
	@CsvSource({ "0, true", "1, true", "2, true", "0, false", "1, false", "2, false" })
	public void testSetElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS, (session, student) -> {
			final Course replacement = course( session, 4L );
			return student.courses.set( index, replacement ).id;
		} );

		final var expected = new TreeMap<>( change.rowsBefore() );
		expected.put( change.rowId(), 4L );
		assertThat( rows( scope, COURSE_ROWS ) ).isEqualTo( expected );
	}

	@ParameterizedTest(name = CASES)
	@CsvSource({ "0, true", "1, true", "2, true", "0, false", "1, false", "2, false" })
	public void testSetBasicElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, NICKNAME_ROWS,
				(session, student) -> student.nicknames.set( index, "replacement" ) );

		final var expected = new TreeMap<>( change.rowsBefore() );
		expected.put( change.rowId(), "replacement" );
		assertThat( rows( scope, NICKNAME_ROWS ) ).isEqualTo( expected );
	}

	/**
	 * An idbag gives the row of a removed element to the element added after it, so this writes the
	 * same update as {@link #testSetElement}.
	 */
	@ParameterizedTest(name = CASES)
	@CsvSource({ "0, true", "1, true", "2, true", "0, false", "1, false", "2, false" })
	public void testRemoveAndAddElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS, (session, student) -> {
			final Course replacement = course( session, 4L );
			final Course removed = student.courses.remove( index );
			student.courses.add( replacement );
			return removed.id;
		} );

		final var expected = new TreeMap<>( change.rowsBefore() );
		expected.put( change.rowId(), 4L );
		assertThat( rows( scope, COURSE_ROWS ) ).isEqualTo( expected );
	}

	@ParameterizedTest(name = CASES)
	@CsvSource({ "0, true", "1, true", "2, true", "0, false", "1, false", "2, false" })
	public void testRemoveElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS,
				(session, student) -> student.courses.remove( index ).id );

		final var expected = new TreeMap<>( change.rowsBefore() );
		expected.remove( change.rowId() );
		assertThat( rows( scope, COURSE_ROWS ) ).isEqualTo( expected );
	}

	@ParameterizedTest(name = "loaded in a later session: {0}")
	@ValueSource(booleans = { true, false })
	public void testAddElement(boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS, (session, student) -> {
			final Course added = course( session, 4L );
			student.courses.add( added );
			return null;
		} );

		final var rows = rows( scope, COURSE_ROWS );
		assertThat( rows ).hasSize( 4 ).containsAllEntriesOf( change.rowsBefore() );
		rows.keySet().removeAll( change.rowsBefore().keySet() );
		assertThat( rows.values() ).containsExactly( 4L );
	}
}
