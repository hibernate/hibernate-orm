package org.hibernate.temporal.audit.collection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = {
		MixedTemporalAssociationTest.Student.class,
		MixedTemporalAssociationTest.Course.class
})
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.collection.MixedTemporalAssociationTest$TxIdSupplier")
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MixedTemporalAssociationTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	private int firstChangeset;
	private int afterMentorship;

	@BeforeAll
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// Rev 1: student enrolled in one course
		sf.inTransaction( session -> {
			var course = new Course( 1L, "Math" );
			session.persist( course );
			var student = new Student( 1L, "Alice" );
			student.courses.add( course );
			session.persist( student );
			var bob = new Student( 2L, "Bob" );
			session.persist( bob );
			student.mentees.add( bob );
		} );
		firstChangeset = currentTxId;

		// Rev 2: enroll in second course
		sf.inTransaction( session -> {
			var course = new Course( 2L, "Physics" );
			session.persist( course );
			var student = session.find( Student.class, 1L );
			student.favorite = course;
			student.courses.add( course );
		} );
		sf.inTransaction( session -> session.find( Course.class, 1L ).name = "Algebra" );
		sf.inTransaction( session -> session.find( Student.class, 1L ).mentees.clear() );
		afterMentorship = currentTxId;
	}

	@Test
	void rightJoinPreservesOnlyVisibleCourses(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			assertEquals( List.of( "Math" ), session.createSelectionQuery(
					"select c.name from Student s right join s.courses c order by c.id", String.class ).list() );
			assertEquals( List.of( 1L ), session.createSelectionQuery(
					"select c.id from Student s right join s.courses c", Long.class ).list() );
		}
	}

	@Test
	void inverseMembershipAtChangeset(SessionFactoryScope scope) {
		var factory = scope.getSessionFactory();
		final String hql = "select size(s.mentors) from Student s where s.id = 2";
		try (var session = factory.withOptions().atChangeset( firstChangeset ).openSession()) {
			assertEquals( 1, session.createSelectionQuery( hql, Integer.class ).getSingleResult() );
			assertEquals( 1L, session.find( Student.class, 2L ).mentors.iterator().next().id );
		}
		try (var session = factory.withOptions().atChangeset( afterMentorship ).openSession()) {
			assertEquals( 0, session.createSelectionQuery( hql, Integer.class ).getSingleResult() );
			assertEquals( 0, session.find( Student.class, 2L ).mentors.size() );
		}
	}

	@Test
	void indexedAccessAtChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			final var courses = session.find( Student.class, 1L ).courses;
			assertEquals( 1, Hibernate.size( courses ) );
			assertEquals( "Math", Hibernate.get( courses, 0 ).name );
			assertNull( Hibernate.get( courses, 1 ) );
			assertFalse( Hibernate.isInitialized( courses ) );
		}
	}

	@Test
	void assignNullableTemporalAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( List.of( 2L ), session.createSelectionQuery(
					"select s.id from Student s where s.favorite is null", Long.class ).list() );
			assertEquals( List.of( 2L ), session.createSelectionQuery(
					"select s.id from Student s where s.favorite.id is null", Long.class ).list() );
			assertEquals( 1, session.createMutationQuery( "update Student set favorite = :course where id = 2" )
					.setParameter( "course", session.getReference( Course.class, 1L ) ).executeUpdate() );
			assertEquals( List.of( 2L ), session.createSelectionQuery(
					"select s.id from Student s where fk(s.favorite) = 1", Long.class ).list() );
		} );
	}

	@Test
	void nullCheckTreatsInvisibleTargetAsNull(SessionFactoryScope scope) {
		// Alice's favorite course did not exist yet at the first changeset
		final String isNull = "select s.id from Student s where s.id = 1 and s.favorite is null";
		final String isNotNull = "select s.id from Student s where s.id = 1 and s.favorite is not null";
		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			assertEquals( List.of( 1L ), session.createSelectionQuery( isNull, Long.class ).list() );
			assertEquals( List.of(), session.createSelectionQuery( isNotNull, Long.class ).list() );
		}
		scope.inSession( session -> {
			assertEquals( List.of(), session.createSelectionQuery( isNull, Long.class ).list() );
			assertEquals( List.of( 1L ), session.createSelectionQuery( isNotNull, Long.class ).list() );
		} );
	}

	@Test
	void dereferenceHistoricalTargetId(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			assertEquals( List.of(), session.createSelectionQuery(
					"select s.favorite.id from Student s where s.id = 1", Long.class ).list() );
			assertEquals( List.of( 1L ), session.createSelectionQuery(
					"select s.id from Student s where fk(s.favorite) = 2", Long.class ).list() );
		}
		scope.inSession( session -> assertEquals( List.of( 2L ), session.createSelectionQuery(
				"select s.favorite.id from Student s where s.id = 1", Long.class ).list() ) );
	}

	@Entity(name = "Student")
	static class Student {
		@Id
		long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Course favorite;
		@Temporal
		@ManyToMany
		@OrderColumn(name = "position_in_list")
		List<Course> courses = new ArrayList<>();
		@Temporal
		@ManyToMany
		Set<Student> mentees = new HashSet<>();
		@ManyToMany(mappedBy = "mentees")
		Set<Student> mentors = new HashSet<>();

		Student() {
		}

		Student(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Temporal
	@Entity(name = "Course")
	static class Course {
		@Id
		long id;
		String name;

		Course() {
		}

		Course(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
