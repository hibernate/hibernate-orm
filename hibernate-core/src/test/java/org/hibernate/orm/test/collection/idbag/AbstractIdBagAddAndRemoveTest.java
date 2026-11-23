package org.hibernate.orm.test.collection.idbag;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adding an element to an idbag inserts one row, removing an element deletes one row, and neither
 * touches the row of any other element, wherever in the list the element is. The tests read the rows
 * back with a native query, since the in-memory list is right whichever rows were written.
 */
@JiraKey("HHH-21244")
public abstract class AbstractIdBagAddAndRemoveTest {
	private static final String INDEXES =
			"0, true\n1, true\n2, true\n3, true\n0, false\n1, false\n2, false\n3, false";
	private static final String CASES = "index {0}, loaded in a later session: {1}";

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

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
		assertThat( rows( scope ) ).isEqualTo( expected );
	}

	/**
	 * Every row written before the change still has its collection id and its element, and there is
	 * one new row for each added course.
	 */
	private static void assertAdded(SessionFactoryScope scope, SortedMap<Long, Long> rowsBefore, Long... added) {
		final var rows = rows( scope );
		assertThat( rows ).containsAllEntriesOf( rowsBefore );
		rows.keySet().removeAll( rowsBefore.keySet() );
		assertThat( rows.values() ).containsExactlyInAnyOrder( added );
	}

	/**
	 * Applies a change to the courses of the student, either in the session which wrote them or in a
	 * later one, and returns the rows of the join table as they were before the change.
	 */
	private static SortedMap<Long, Long> change(
			SessionFactoryScope scope,
			boolean loaded,
			BiConsumer<SessionImplementor, List<Course>> change) {
		if ( loaded ) {
			scope.inTransaction( AbstractIdBagAddAndRemoveTest::persistStudent );
			final var rowsBefore = rows( scope );
			scope.inTransaction( session -> change.accept( session, session.find( Student.class, 1L ).courses ) );
			return rowsBefore;
		}
		else {
			return scope.fromTransaction( session -> {
				final Student student = persistStudent( session );
				session.flush();
				final var rowsBefore = rows( session );
				change.accept( session, student.courses );
				return rowsBefore;
			} );
		}
	}

	private static Student persistStudent(SessionImplementor session) {
		final Student student = new Student();
		for ( long id = 1; id <= 3; id++ ) {
			student.courses.add( course( session, id ) );
		}
		session.persist( student );
		return student;
	}

	private static Course course(SessionImplementor session, Long id) {
		final Course course = new Course( id );
		session.persist( course );
		return course;
	}

	private static SortedMap<Long, Long> rows(SessionFactoryScope scope) {
		return scope.fromTransaction( AbstractIdBagAddAndRemoveTest::rows );
	}

	/**
	 * The rows of the join table, as the id of the course by collection id.
	 */
	private static SortedMap<Long, Long> rows(SessionImplementor session) {
		final SortedMap<Long, Long> rows = new TreeMap<>();
		final String sql = "select id, course_id from student_courses where student_id = 1";
		for ( Object[] row : session.createNativeQuery( sql, Object[].class ).getResultList() ) {
			// the Java type of an integer column read by a native query depends on the database
			rows.put( ( (Number) row[0] ).longValue(), ( (Number) row[1] ).longValue() );
		}
		return rows;
	}

	@Entity(name = "Student")
	@Table(name = "students")
	@SequenceGenerator(name = "student_courses_seq", sequenceName = "student_courses_seq")
	public static class Student {
		@Id
		Long id = 1L;

		@ManyToMany
		@JoinTable(
				name = "student_courses",
				joinColumns = @JoinColumn(name = "student_id"),
				inverseJoinColumns = @JoinColumn(name = "course_id")
		)
		@CollectionId(column = @Column(name = "id"), generator = "student_courses_seq")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		List<Course> courses = new ArrayList<>();
	}

	@Entity(name = "Course")
	@Table(name = "courses")
	public static class Course {
		@Id
		Long id;

		Course() {
		}

		Course(Long id) {
			this.id = id;
		}
	}
}
