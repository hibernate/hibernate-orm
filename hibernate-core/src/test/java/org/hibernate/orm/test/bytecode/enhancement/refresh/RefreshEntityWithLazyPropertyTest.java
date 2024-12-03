/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.refresh;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.SkipForDialectGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Jira("HHH-13377")
@DomainModel(
		annotatedClasses = {
				RefreshEntityWithLazyPropertyTest.Person.class,
				RefreshEntityWithLazyPropertyTest.Course.class,
				RefreshEntityWithLazyPropertyTest.Position.class}
)
@SessionFactory
@BytecodeEnhanced
@SkipForDialectGroup(
		{
				@SkipForDialect( dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "does not support || as String concatenation"),
				@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "does not support || as String concatenation"),
		}
)
public class RefreshEntityWithLazyPropertyTest {

	private static final Long PERSON_ID = 1L;
	private static final Long ASSISTANT_PROFESSOR_POSITION_ID = 1L;
	private static final Long PROFESSOR_POSITION_ID = 2L;
	private static final String ASSISTANT_POSITION_DESCRIPTION = "Assistant Professor";
	private static final String POSITION_DESCRIPTION = "Professor";
	private static final String PROFESSOR_FIRST_NAME = "John";
	private static final String PROFESSOR_LAST_NAME = "Doe";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Position professorPosition = new Position( PROFESSOR_POSITION_ID, POSITION_DESCRIPTION );
			session.persist( professorPosition );

			Position assistantProfessor = new Position( ASSISTANT_PROFESSOR_POSITION_ID,
					ASSISTANT_POSITION_DESCRIPTION );
			session.persist( assistantProfessor );

			Person person = new Person( PERSON_ID, PROFESSOR_FIRST_NAME, PROFESSOR_LAST_NAME, assistantProfessor,
					professorPosition );
			session.persist( person );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Course" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
			session.createMutationQuery( "delete from Position" ).executeUpdate();
		} );
	}

	@Test
	public void testRefreshOfLazyField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			assertThat( p.getLastName() ).isEqualTo( PROFESSOR_LAST_NAME );

			String updatedLastName = "Johnson";
			session.createMutationQuery( "update Person p " +
										"set p.lastName = :lastName " +
										"where p.id = :id"
					)
					.setParameter( "lastName", updatedLastName )
					.setParameter( "id", PERSON_ID )
					.executeUpdate();

			session.refresh( p );
			assertThat( p.getLastName() ).isEqualTo( updatedLastName );
		} );
	}

	@Test
	public void testRefreshOfLazyFormula(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			assertThat( p.getFullName() ).isEqualTo( "John Doe" );

			p.setLastName( "Johnson" );
			session.flush();
			session.refresh( p );
			assertThat( p.getFullName() ).isEqualTo( "John Johnson" );
		} );
	}

	@Test
	public void testRefreshOfLazyOneToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			assertThat( p.getCourses().size() ).isEqualTo( 0 );

			session.createMutationQuery( "insert into Course (id, title, person) values (:id, :title, :person) " )
					.setParameter( "id", 0 )
					.setParameter( "title", "Book Title" )
					.setParameter( "person", p )
					.executeUpdate();

			session.refresh( p );
			assertThat( p.getCourses().size() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testRefreshOfLazyManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			assertThat( p.getPosition().id ).isEqualTo( ASSISTANT_PROFESSOR_POSITION_ID );

			Position professorPosition = session.find( Position.class, PROFESSOR_POSITION_ID );

			session.createMutationQuery(
							"update Person p " +
							"set p.position = :position " +
							"where p.id = :personId "
					)
					.setParameter( "position", professorPosition )
					.setParameter( "personId", p.getId() )
					.executeUpdate();

			session.refresh( p );
			assertThat( p.getPosition().id ).isEqualTo( PROFESSOR_POSITION_ID );

		} );
	}

	@Test
	public void testRefreshOfLazyManyToOneCascadeRefresh(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			Position position = p.getPosition();
			assertThat( position.getId() ).isEqualTo( ASSISTANT_PROFESSOR_POSITION_ID );
			assertThat( position.getDescription() ).isEqualTo( ASSISTANT_POSITION_DESCRIPTION );

			String newAssistantProfessorDescription = "Assistant Professor 2";
			session.createMutationQuery(
							"update Position " +
							"set description = :description " +
							"where id = :id "
					)
					.setParameter( "description", newAssistantProfessorDescription )
					.setParameter( "id", ASSISTANT_PROFESSOR_POSITION_ID )
					.executeUpdate();

			session.refresh( p );
			// the association has been refreshed because it's annotated with `cascade = CascadeType.REFRESH`
			assertThat( p.getPosition().getDescription() ).isEqualTo( newAssistantProfessorDescription );
		} );
	}

	@Test
	public void testRefreshOfLazyManyToOneNoCascadeRefresh(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = session.find( Person.class, PERSON_ID );
			Position position = p.getPreviousPosition();
			assertThat( position.getId() ).isEqualTo( PROFESSOR_POSITION_ID );
			assertThat( position.getDescription() ).isEqualTo( POSITION_DESCRIPTION );

			String newAssistantProfessorDescription = "Assistant Professor 2";
			session.createMutationQuery(
							"update Position " +
							"set description = :description " +
							"where id = :id "
					)
					.setParameter( "description", newAssistantProfessorDescription )
					.setParameter( "id", PROFESSOR_POSITION_ID )
					.executeUpdate();

			session.refresh( p );
			// the association has not been refreshed because it's not annotated with `cascade = CascadeType.REFRESH`
			assertThat( p.getPreviousPosition().getDescription() ).isEqualTo( POSITION_DESCRIPTION );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		@Basic(fetch = FetchType.LAZY)
		private String lastName;

		@Basic(fetch = FetchType.LAZY)
		@Formula("firstName || ' ' || lastName")
		private String fullName;

		@OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, orphanRemoval = true)
		private Set<Course> courses = new HashSet<>();

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
		private Position position;

		@ManyToOne(fetch = FetchType.LAZY)
		private Position previousPosition;

		protected Person() {
		}

		public Person(Long id, String firstName, String lastName, Position position, Position previousPosition) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
			this.position = position;
			this.previousPosition = previousPosition;
		}

		public Long getId() {
			return id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getFullName() {
			return fullName;
		}

		public Set<Course> getCourses() {
			return courses;
		}

		public Position getPosition() {
			return position;
		}

		public Position getPreviousPosition() {
			return previousPosition;
		}
	}

	@Entity(name = "Course")
	public static class Course {

		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		private Person person;

		protected Course() {
		}

		public Course(Long id, String title, Person person) {
			this.id = id;
			this.title = title;
			this.person = person;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Person getPerson() {
			return person;
		}
	}

	@Entity(name = "Position")
	@Table(name = "POSITION_TABLE")
	public static class Position {

		@Id
		private Long id;

		@Basic(fetch = FetchType.LAZY)
		private String description;

		public Position() {
		}

		public Position(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
