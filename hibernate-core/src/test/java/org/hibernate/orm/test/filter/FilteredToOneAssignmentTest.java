package org.hibernate.orm.test.filter;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An assignment to a to-one whose target has an enabled load-by-key filter
 * writes the foreign key, rather than the identifier of the joined target.
 */
@DomainModel(annotatedClasses = { FilteredToOneAssignmentTest.Student.class, FilteredToOneAssignmentTest.Course.class })
@SessionFactory
@JiraKey("HHH-21216")
class FilteredToOneAssignmentTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var math = new Course( 1 );
			session.persist( math );
			session.persist( new Course( 5 ) );
			session.persist( new Student( 2, null ) );
			session.persist( new Student( 3, math ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void assignmentWritesForeignKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibleCourses" );
			assertEquals( 1, session.createMutationQuery( "update Student set favorite = :course where id = 2" )
					.setParameter( "course", session.getReference( Course.class, 1 ) )
					.executeUpdate() );
			assertEquals( 1, session.createMutationQuery( "update Student set favorite = :course where id = 3" )
					.setParameter( "course", session.getReference( Course.class, 5 ) )
					.executeUpdate() );
		} );
		scope.inTransaction( session -> {
			assertEquals( List.of( 2, 3 ),
					session.createNativeQuery( "select id from students order by id", Integer.class ).list() );
			assertEquals( 1, session.createNativeQuery( "select favorite_id from students where id = 2", Integer.class )
					.getSingleResult() );
			assertEquals( 5, session.createNativeQuery( "select favorite_id from students where id = 3", Integer.class )
					.getSingleResult() );
		} );
	}

	@Entity(name = "Student")
	@Table(name = "students")
	static class Student {
		@Id
		int id;
		@ManyToOne(fetch = FetchType.LAZY)
		Course favorite;

		Student() {
		}

		Student(int id, Course favorite) {
			this.id = id;
			this.favorite = favorite;
		}
	}

	@Entity(name = "Course")
	@Table(name = "courses")
	@FilterDef(name = "visibleCourses", applyToLoadByKey = true)
	@Filter(name = "visibleCourses", condition = "id > 0")
	static class Course {
		@Id
		int id;

		Course() {
		}

		Course(int id) {
			this.id = id;
		}
	}
}
