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

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;

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

@DomainModel(annotatedClasses = { IdBagFixture.Student.class, IdBagFixture.Course.class })
@SessionFactory
abstract class IdBagFixture {
	static final String COURSE_ROWS = "select id, course_id from student_courses where student_id = 1";
	static final String NICKNAME_ROWS = "select id, nickname from student_nicknames where student_id = 1";

	static final String CASES = "index {0}, loaded in a later session: {1}";

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	/**
	 * Applies a change to the collections of the student, either in the session which wrote them or
	 * in a later one. The change returns the element it replaced or removed, as the value held by the
	 * row of that element.
	 */
	static Change change(
			SessionFactoryScope scope,
			boolean loaded,
			String rowsSql,
			BiFunction<SessionImplementor, Student, Object> change) {
		if ( loaded ) {
			scope.inTransaction( IdBagFixture::persistStudent );
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
			student.courses.add( course( session, id ) );
			student.nicknames.add( "n" + id );
		}
		session.persist( student );
		return student;
	}

	static Course course(SessionImplementor session, long id) {
		final Course course = new Course();
		course.id = id;
		session.persist( course );
		return course;
	}

	static SortedMap<Long, Object> rows(SessionFactoryScope scope, String sql) {
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

	record Change(SortedMap<Long, Object> rowsBefore, Object element) {
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
	}
}
