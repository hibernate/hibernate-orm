package org.hibernate.orm.test.stream;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Eager unidirectional collections remain available after consuming the entire scroll and closing the session.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { EagerOneToManyScrollTest.Student.class, EagerOneToManyScrollTest.Course.class })
@SessionFactory
@JiraKey("HHH-16405")
public class EagerOneToManyScrollTest {

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int studentId = 1; studentId <= 2; studentId++ ) {
				final var student = new Student();
				student.id = studentId;
				for ( int courseNumber = 1; courseNumber <= 2; courseNumber++ ) {
					final var course = new Course();
					course.name = "course-" + studentId + "-" + courseNumber;
					student.courses.add( course );
				}
				session.persist( student );
			}
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testEagerCollectionsAfterFullScrollAndClose(SessionFactoryScope scope) {
		final List<Student> students = scope.fromSession( session -> {
			final List<Student> collected = new ArrayList<>();
			try ( var results = session.createQuery(
					"from ScrollStudent order by id", Student.class
			).scroll( ScrollMode.FORWARD_ONLY ) ) {
				while ( results.next() ) {
					collected.add( results.get() );
				}
			}
			return collected;
		} );

		assertThat( students ).extracting( student -> student.id ).containsExactly( 1, 2 );
		for ( Student student : students ) {
			assertThat( Hibernate.isInitialized( student.courses ) ).isTrue();
			assertThat( student.courses ).extracting( course -> course.name ).containsExactlyInAnyOrder(
					"course-" + student.id + "-1", "course-" + student.id + "-2"
			);
			assertThat( student.courses ).extracting( course -> course.id ).doesNotContainNull().doesNotHaveDuplicates();
		}
	}

	@Entity(name = "ScrollStudent")
	@Table(name = "hhh16405_student")
	public static class Student {
		@Id
		private Integer id;

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@OrderColumn(name = "course_position")
		private List<Course> courses = new ArrayList<>();
	}

	@Entity(name = "ScrollCourse")
	@Table(name = "hhh16405_course")
	public static class Course {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;
	}
}
