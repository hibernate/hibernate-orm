package org.hibernate.orm.test.filter;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A null check on a to-one whose target has an enabled load-by-key filter
 * treats a hidden target like a missing one, and still matches a null
 * foreign key.
 */
@DomainModel(annotatedClasses = { FilteredToOneNullCheckTest.Student.class, FilteredToOneNullCheckTest.Course.class })
@SessionFactory
@JiraKey("HHH-21216")
class FilteredToOneNullCheckTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = new Course( 1 );
			final var hidden = new Course( 5 );
			session.persist( visible );
			session.persist( hidden );
			session.persist( new Student( 2, null ) );
			session.persist( new Student( 3, visible ) );
			session.persist( new Student( 4, hidden ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void nullCheckWithFilter(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.enableFilter( "visibleCourses" );
			assertEquals( List.of( 2, 4 ), session.createSelectionQuery(
					"select s.id from Student s where s.favorite is null order by s.id", Integer.class ).list() );
			assertEquals( List.of( 3 ), session.createSelectionQuery(
					"select s.id from Student s where s.favorite is not null order by s.id", Integer.class ).list() );
			assertEquals( List.of( 2 ), session.createSelectionQuery(
					"select s.id from Student s where fk(s.favorite) is null order by s.id", Integer.class ).list() );
		} );
	}

	@Test
	void nullCheckWithSqlRestriction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( List.of( 2, 4 ), session.createSelectionQuery(
					"select s.id from Student s where s.restrictedFavorite is null order by s.id", Integer.class ).list() );
			assertEquals( List.of( 3 ), session.createSelectionQuery(
					"select s.id from Student s where s.restrictedFavorite is not null", Integer.class ).list() );
			assertEquals( List.of( 2 ), session.createSelectionQuery(
					"select s.id from Student s where fk(s.restrictedFavorite) is null", Integer.class ).list() );
		} );
	}

	@Test
	void nullCheckWithoutFilter(SessionFactoryScope scope) {
		scope.inSession( session -> assertEquals( List.of( 2 ), session.createSelectionQuery(
				"select s.id from Student s where s.favorite is null order by s.id", Integer.class ).list() ) );
	}

	@Entity(name = "Student")
	@Table(name = "students")
	static class Student {
		@Id
		int id;
		@ManyToOne(fetch = FetchType.LAZY)
		Course favorite;
		@ManyToOne(fetch = FetchType.LAZY)
		@SQLRestriction("id <> 5")
		Course restrictedFavorite;

		Student() {
		}

		Student(int id, Course favorite) {
			this.id = id;
			this.favorite = favorite;
			this.restrictedFavorite = favorite;
		}
	}

	@Entity(name = "Course")
	@Table(name = "courses")
	@FilterDef(name = "visibleCourses", applyToLoadByKey = true)
	@Filter(name = "visibleCourses", condition = "id <> 5")
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
