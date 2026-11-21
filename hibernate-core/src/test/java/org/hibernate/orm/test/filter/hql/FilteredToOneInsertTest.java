package org.hibernate.orm.test.filter.hql;

import java.util.List;

import jakarta.persistence.Entity;
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

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = { FilteredToOneInsertTest.Student.class, FilteredToOneInsertTest.Course.class })
@SessionFactory
@JiraKey("HHH-21225")
class FilteredToOneInsertTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = new Course( 1 );
			final var hidden = new Course( 2 );
			session.persist( visible );
			session.persist( hidden );
			session.persist( new Student( 11, visible ) );
			session.persist( new Student( 12, hidden ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void insertValues(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibleCourses" );
			assertEquals( 1, session.createMutationQuery( "insert into Student (id, favorite) values (20, :course)" )
					.setParameter( "course", session.getReference( Course.class, 2 ) )
					.executeUpdate() );
			assertEquals( 2, session.createNativeQuery(
					"select favorite_id from students where id = 20", Integer.class ).getSingleResult() );
		} );
	}

	@Test
	void insertSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibleCourses" );
			assertEquals( 1, session.createMutationQuery(
					"insert into Student (id, favorite) select s.id + 100, s.favorite from Student s where s.favorite.id > 0"
			).executeUpdate() );
			assertEquals( List.of( 1 ), session.createNativeQuery(
					"select favorite_id from students where id > 100", Integer.class ).list() );
		} );
	}

	@Entity(name = "Student")
	@Table(name = "students")
	static class Student {
		@Id int id;
		@ManyToOne(fetch = LAZY) Course favorite;

		Student() {
		}

		Student(int id, Course favorite) {
			this.id = id;
			this.favorite = favorite;
		}
	}

	@Entity(name = "Course")
	@FilterDef(name = "visibleCourses", applyToLoadByKey = true)
	@Filter(name = "visibleCourses", condition = "id = 1")
	static class Course {
		@Id int id;

		Course() {
		}

		Course(int id) {
			this.id = id;
		}
	}
}
