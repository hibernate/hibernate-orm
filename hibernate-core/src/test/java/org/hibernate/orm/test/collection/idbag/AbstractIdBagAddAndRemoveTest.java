package org.hibernate.orm.test.collection.idbag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Adding an element to an idbag inserts one row, removing an element deletes one row, and neither
 * touches the row of any other element, wherever in the list the element is. The tests read the rows
 * back with a native query, since the in-memory list is right whichever rows were written.
 */
@JiraKey("HHH-21244")
public abstract class AbstractIdBagAddAndRemoveTest extends IdBagFixture {
	private static final String INDEXES =
			"0, true\n1, true\n2, true\n3, true\n0, false\n1, false\n2, false\n3, false";

	@ParameterizedTest(name = CASES)
	@CsvSource(textBlock = INDEXES)
	public void testAdd(int index, boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded,
				(session, courses) -> courses.add( index, course( session, 4L ) ) );

		assertAdded( scope, rowsBefore, 4L );
	}

	@ParameterizedTest(name = CASES)
	@CsvSource(textBlock = INDEXES)
	public void testAddAll(int index, boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded,
				(session, courses) -> courses.addAll( index, List.of( course( session, 4L ), course( session, 5L ) ) ) );

		assertAdded( scope, rowsBefore, 4L, 5L );
	}

	@ParameterizedTest(name = CASES)
	@CsvSource(textBlock = INDEXES)
	public void testListIteratorAdd(int index, boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded, (session, courses) -> {
			final ListIterator<Course> iterator = courses.listIterator( index );
			iterator.add( course( session, 4L ) );
			iterator.add( course( session, 5L ) );
		} );

		assertAdded( scope, rowsBefore, 4L, 5L );
	}

	/**
	 * An element added since the last flush has no collection id yet.
	 */
	@ParameterizedTest(name = CASES)
	@CsvSource(textBlock = INDEXES)
	public void testAddBeforeNewElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded, (session, courses) -> {
			courses.add( course( session, 4L ) );
			courses.add( index, course( session, 5L ) );
		} );

		assertAdded( scope, rowsBefore, 4L, 5L );
	}

	@ParameterizedTest(name = "loaded in a later session: {0}")
	@ValueSource(booleans = { true, false })
	public void testAddAfterRemovingNewElement(boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded, (session, courses) -> {
			courses.add( course( session, 4L ) );
			courses.remove( 3 );
			courses.add( course( session, 5L ) );
		} );

		assertAdded( scope, rowsBefore, 5L );
	}

	@ParameterizedTest(name = "loaded in a later session: {0}")
	@ValueSource(booleans = { true, false })
	public void testIteratorRemove(boolean loaded, SessionFactoryScope scope) {
		final List<Long> removed = new ArrayList<>();
		final var rowsBefore = change( scope, loaded, (session, courses) -> {
			final Iterator<Course> iterator = courses.iterator();
			removed.add( iterator.next().id );
			iterator.remove();
			removed.add( iterator.next().id );
			iterator.remove();
		} );

		final var expected = new TreeMap<>( rowsBefore );
		expected.values().removeAll( removed );
		assertThat( rows( scope, COURSE_ROWS ) ).isEqualTo( expected );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testRejectedIndexedAdd(boolean loaded, SessionFactoryScope scope) {
		final var rowsBefore = change( scope, loaded, (session, courses) -> {
			final Course added = course( session, 4L );
			assertThrows( IndexOutOfBoundsException.class, () -> courses.add( -1, added ) );
			courses.add( added );
		} );

		assertAdded( scope, rowsBefore, 4L );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testRemoveAfterFailedPrevious(boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS, (session, student) -> {
			final var courses = student.courses;
			final var iterator = courses.listIterator( 1 );
			final Course removed = iterator.previous();
			assertThrows( NoSuchElementException.class, iterator::previous );
			iterator.remove();
			return removed.id;
		} );

		final var expected = new TreeMap<>( change.rowsBefore() );
		expected.remove( change.rowId() );
		assertThat( rows( scope, COURSE_ROWS ) ).isEqualTo( expected );
	}

	/**
	 * Every row written before the change still has its collection id and its element, and there is
	 * one new row for each added course.
	 */
	private static void assertAdded(SessionFactoryScope scope, SortedMap<Long, Object> rowsBefore, Long... added) {
		final var rows = rows( scope, COURSE_ROWS );
		assertThat( rows ).containsAllEntriesOf( rowsBefore );
		rows.keySet().removeAll( rowsBefore.keySet() );
		assertThat( rows.values() ).containsExactlyInAnyOrder( added );
	}

	/**
	 * Applies a change to the courses of the student, either in the session which wrote them or in a
	 * later one, and returns the rows of the join table as they were before the change.
	 */
	private static SortedMap<Long, Object> change(
			SessionFactoryScope scope,
			boolean loaded,
			BiConsumer<SessionImplementor, List<Course>> change) {
		return change( scope, loaded, COURSE_ROWS, (session, student) -> {
			change.accept( session, student.courses );
			return null;
		} ).rowsBefore();
	}
}
