/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Lansana DIOMANDE
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphWithInheritanceTest.Person.class,
				EntityGraphWithInheritanceTest.Student.class,
				EntityGraphWithInheritanceTest.Teacher.class,
				EntityGraphWithInheritanceTest.School.class,
				EntityGraphWithInheritanceTest.Course.class,
				EntityGraphWithInheritanceTest.FreeCourse.class,
				EntityGraphWithInheritanceTest.PayingCourse.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-18714")
public class EntityGraphWithInheritanceTest {

	private static Long STUDENT_ID = 1L;

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var frenchTeacher = new Teacher();
			frenchTeacher.firstname = "John 2";
			frenchTeacher.lastname = "DOE";
			frenchTeacher.id = 2L;

			var frenchCourse = new PayingCourse();
			frenchCourse.id = 1L;
			frenchCourse.name = "French";
			frenchCourse.moneyReceiver = frenchTeacher;

			var student = new Student();
			student.id = STUDENT_ID;
			student.firstname = "John";
			student.lastname = "Doe";

			var school = new School();
			school.name = "Fake";
			school.id = 1l;

			student.school = school;

			student.courses = new ArrayList<>() {{
				add( frenchCourse );
			}};

			session.persist( student );
		} );
	}

	@AfterAll
	public void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testWithoutGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var person = session.find(
							Person.class,
							STUDENT_ID
					);

					session.detach( person );

					assertThat( person ).isInstanceOf( Student.class );

					var student = (Student) person;

					assertThat( Hibernate.isInitialized( student.courses ) ).isFalse();
					assertThat( Hibernate.isInitialized( student.school ) ).isFalse();
				}
		);
	}

	@Test
	public void testWhenGraphHasSubclassSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					var graph = session.createEntityGraph( Person.class );

					var studentSubGraph = graph.addTreatedSubgraph( Student.class );
					studentSubGraph.addAttributeNode( "school" );
					studentSubGraph.addAttributeNode( "courses" );


					var person = session.find(
							Person.class,
							STUDENT_ID,
							Collections.singletonMap( GraphSemantic.FETCH.getJakartaHintName(), graph )
					);

					session.detach( person );

					assertThat( person ).isInstanceOf( Student.class );

					var student = (Student) person;

					assertThat( Hibernate.isInitialized( student.school ) ).isTrue();
					assertThat( Hibernate.isInitialized( student.courses ) ).isTrue();
					assertThat( student.courses ).allSatisfy( course -> {
						assertThat( course ).isInstanceOf( PayingCourse.class );
						var payingCourse = (PayingCourse) course;
						assertThat( Hibernate.isInitialized( payingCourse.moneyReceiver ) ).isFalse();
					} );
				}
		);
	}


	@Test
	public void testWhenGraphHasSubclassSubgraphLinkedWithSubgraphThatHasInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					var graph = session.createEntityGraph( Person.class );

					var studentSubGraph = graph.addTreatedSubgraph( Student.class );
					studentSubGraph.addAttributeNode( "school" );
					studentSubGraph.addAttributeNode( "courses" );

					var payingCourseSubgraph = studentSubGraph
							.addSubgraph( "courses", PayingCourse.class );

					payingCourseSubgraph.addSubgraph( "moneyReceiver" );

					var person = session.find(
							Person.class,
							STUDENT_ID,
							Collections.singletonMap( GraphSemantic.FETCH.getJakartaHintName(), graph )
					);

					session.detach( person );

					assertThat( person ).isInstanceOf( Student.class );

					var student = (Student) person;

					assertThat( Hibernate.isInitialized( student.school ) ).isTrue();
					assertThat( Hibernate.isInitialized( student.courses ) ).isTrue();
					assertThat( student.courses ).allSatisfy( course -> {
						assertThat( course ).isInstanceOf( PayingCourse.class );
						var payingCourse = (PayingCourse) course;
						assertThat( Hibernate.isInitialized( payingCourse.moneyReceiver ) ).isTrue();
					} );
				}
		);
	}


	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "person_type", discriminatorType = DiscriminatorType.STRING)
	public static abstract class Person {
		@Id
		Long id;
		String firstname;
		String lastname;

	}

	@Entity(name = "Student")
	@DiscriminatorValue("student")
	public static class Student extends Person {

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		School school;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		List<Course> courses;
	}


	@Entity(name = "Teacher")
	@DiscriminatorValue("teacher")
	public static class Teacher extends Person {
	}


	@Entity(name = "School")
	public static class School {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "Course")
	@DiscriminatorColumn(name = "course_type", discriminatorType = DiscriminatorType.STRING)
	public static abstract class Course {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "FreeCourse")
	public static class FreeCourse extends Course {
	}

	@Entity(name = "PayingCourse")
	@DiscriminatorValue("paying")
	public static class PayingCourse extends Course {

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		Teacher moneyReceiver;
	}


}
