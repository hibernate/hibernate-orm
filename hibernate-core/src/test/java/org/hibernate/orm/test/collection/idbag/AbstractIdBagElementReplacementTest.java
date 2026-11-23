package org.hibernate.orm.test.collection.idbag;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replacing an element of an idbag updates the row which held the replaced element, and leaves every
 * other row of the collection table alone. The tests read the rows back with a native query, since the
 * in-memory list is right whichever row was written.
 */
@JiraKey("HHH-21243")
public abstract class AbstractIdBagElementReplacementTest {
	private static final String COURSE_ROWS = "select id, course_id from student_courses where student_id = 1";
	private static final String NICKNAME_ROWS = "select id, nickname from student_nicknames where student_id = 1";

	private static final String CASES = "index {0}, loaded in a later session: {1}";

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest(name = CASES)
	@CsvSource({ "0, true", "1, true", "2, true", "0, false", "1, false", "2, false" })
	public void testSetElement(int index, boolean loaded, SessionFactoryScope scope) {
		final var change = change( scope, loaded, COURSE_ROWS, (session, student) -> {
			final Course replacement = new Course( 4L, "replacement" );
			session.persist( replacement );
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
			final Course replacement = new Course( 4L, "replacement" );
			session.persist( replacement );
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
			final Course added = new Course( 4L, "added" );
			session.persist( added );
			student.courses.add( added );
			return null;
		} );

		final var rows = rows( scope, COURSE_ROWS );
		assertThat( rows ).hasSize( 4 ).containsAllEntriesOf( change.rowsBefore() );
		rows.keySet().removeAll( change.rowsBefore().keySet() );
		assertThat( rows.values() ).containsExactly( 4L );
	}

	/**
	 * Applies a change to the collections of the student, either in the session which wrote them or
	 * in a later one. The change returns the element it replaced or removed, as the value held by the
	 * row of that element.
	 */
	private static Change change(
			SessionFactoryScope scope,
			boolean loaded,
			String rowsSql,
			BiFunction<SessionImplementor, Student, Object> change) {
		if ( loaded ) {
			scope.inTransaction( AbstractIdBagElementReplacementTest::persistStudent );
			final var rowsBefore = rows( scope, rowsSql );
			return new Change( rowsBefore,
					scope.fromTransaction( session -> change.apply( session, session.find( Student.class, 1L ) ) ) );
		}
		else {
			return scope.fromTransaction( session -> {
				final Student student = persistStudent( session );
				session.flush();
				return new Change( rows( session, rowsSql ), change.apply( session, student ) );
			} );
		}
	}

	private static Student persistStudent(SessionImplementor session) {
		final Student student = new Student();
		for ( long id = 1; id <= 3; id++ ) {
			final Course course = new Course( id, "c" + id );
			session.persist( course );
			student.courses.add( course );
			student.nicknames.add( "n" + id );
		}
		session.persist( student );
		return student;
	}

	private static SortedMap<Long, Object> rows(SessionFactoryScope scope, String sql) {
		return scope.fromTransaction( session -> rows( session, sql ) );
	}

	/**
	 * The rows of a collection table, as the value of the element column by collection id.
	 */
	private static SortedMap<Long, Object> rows(SessionImplementor session, String sql) {
		final SortedMap<Long, Object> rows = new TreeMap<>();
		for ( Object[] row : session.createNativeQuery( sql, Object[].class ).getResultList() ) {
			// the Java type of an integer column read by a native query depends on the database
			rows.put( ( (Number) row[0] ).longValue(), row[1] instanceof Number number ? number.longValue() : row[1] );
		}
		return rows;
	}

	private record Change(SortedMap<Long, Object> rowsBefore, Object element) {
		/**
		 * The collection id of the row which held the replaced or removed element.
		 */
		Long rowId() {
			return rowsBefore.entrySet().stream()
					.filter( row -> Objects.equals( row.getValue(), element ) )
					.map( Map.Entry::getKey )
					.findFirst().orElseThrow();
		}
	}

	@Entity(name = "Student")
	@Table(name = "students")
	// a collection id is never the position of an element
	@SequenceGenerator(name = "collection_id_seq", sequenceName = "collection_id_seq", initialValue = 100)
	public static class Student {
		@Id
		Long id = 1L;

		@ManyToMany
		@JoinTable(
				name = "student_courses",
				joinColumns = @JoinColumn(name = "student_id"),
				inverseJoinColumns = @JoinColumn(name = "course_id")
		)
		@CollectionId(column = @Column(name = "id"), generator = "collection_id_seq")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		List<Course> courses = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "student_nicknames", joinColumns = @JoinColumn(name = "student_id"))
		@Column(name = "nickname")
		@CollectionId(column = @Column(name = "id"), generator = "collection_id_seq")
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		List<String> nicknames = new ArrayList<>();
	}

	@Entity(name = "Course")
	@Table(name = "courses")
	public static class Course {
		@Id
		Long id;

		String title;

		Course() {
		}

		Course(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
