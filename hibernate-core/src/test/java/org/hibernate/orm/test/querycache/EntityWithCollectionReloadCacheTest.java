/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EntityWithCollectionReloadCacheTest.Course.class,
		EntityWithCollectionReloadCacheTest.Subject.class,
		EntityWithCollectionReloadCacheTest.Demand.class,
		EntityWithCollectionReloadCacheTest.Student.class,
		EntityWithCollectionReloadCacheTest.Major.class,
		EntityWithCollectionReloadCacheTest.StudentMajor.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18478" )
public class EntityWithCollectionReloadCacheTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( String subject : new String[] { "MATH", "BIOL", "CS" } ) {
				final List<Tuple> resultList = session.createQuery(
								"select d.id, d.course, s " +
										"from Demand d inner join d.student s left join fetch s.majors " +
										"where d.course.subject.name = :subject",
								Tuple.class
						)
						.setParameter( "subject", subject )
						.setCacheable( true )
						.getResultList();
				assertThat( resultList ).hasSize( subject.equals( "MATH" ) ? 5 : 4 ).allSatisfy( tuple -> {
					assertThat( tuple.get( 1, Course.class ).getSubject().getName() ).isEqualTo( subject );
					assertThat( tuple.get( 2, Student.class ).getMajors() ).matches( Hibernate::isInitialized );
				} );
			}
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Create two majors
			final Major m1 = new Major();
			m1.setName( "Biology" );
			session.persist( m1 );
			final Major m2 = new Major();
			m2.setName( "Computer Science" );
			session.persist( m2 );

			// Create three students
			final Student s1 = new Student();
			s1.setName( "Andrew" );
			final StudentMajor sm1 = new StudentMajor();
			sm1.setStudent( s1 );
			sm1.setMajor( m1 );
			sm1.setClassification( "01" );
			s1.addToMajors( sm1 );
			session.persist( s1 );

			final Student s2 = new Student();
			s2.setName( "Brian" );
			final StudentMajor sm2 = new StudentMajor();
			sm2.setStudent( s2 );
			sm2.setMajor( m1 );
			sm2.setClassification( "02" );
			s2.addToMajors( sm2 );
			session.persist( s2 );

			final Student s3 = new Student();
			s3.setName( "Charlie" );
			final StudentMajor sm3 = new StudentMajor();
			sm3.setStudent( s3 );
			sm3.setMajor( m1 );
			sm3.setClassification( "01" );
			s3.addToMajors( sm3 );
			final StudentMajor sm4 = new StudentMajor();
			sm4.setStudent( s3 );
			sm4.setMajor( m2 );
			sm4.setClassification( "02" );
			s3.addToMajors( sm4 );
			session.persist( s3 );

			// Create two subjects
			final Subject math = new Subject();
			math.setName( "MATH" );
			session.persist( math );
			final Subject biology = new Subject();
			biology.setName( "BIOL" );
			session.persist( biology );
			final Subject cs = new Subject();
			cs.setName( "CS" );
			session.persist( cs );

			// Create a few courses
			final Course c1 = new Course();
			c1.setSubject( math );
			c1.setNumber( "101" );
			session.persist( c1 );
			final Course c2 = new Course();
			c2.setSubject( math );
			c2.setNumber( "201" );
			session.persist( c2 );
			final Course c3 = new Course();
			c3.setSubject( biology );
			c3.setNumber( "101" );
			session.persist( c3 );
			final Course c4 = new Course();
			c4.setSubject( biology );
			c4.setNumber( "201" );
			session.persist( c4 );
			final Course c5 = new Course();
			c5.setSubject( cs );
			c5.setNumber( "101" );
			session.persist( c5 );
			final Course c6 = new Course();
			c6.setSubject( cs );
			c6.setNumber( "201" );
			session.persist( c6 );

			// Create some course demands
			final Demand d1 = new Demand();
			d1.setCourse( c1 );
			d1.setStudent( s1 );
			session.persist( d1 );
			final Demand d2 = new Demand();
			d2.setCourse( c1 );
			d2.setStudent( s2 );
			session.persist( d2 );
			final Demand d3 = new Demand();
			d3.setCourse( c2 );
			d3.setStudent( s2 );
			session.persist( d3 );
			final Demand d4 = new Demand();
			d4.setCourse( c2 );
			d4.setStudent( s3 );
			session.persist( d4 );
			final Demand d5 = new Demand();
			d5.setCourse( c3 );
			d5.setStudent( s1 );
			session.persist( d5 );
			final Demand d6 = new Demand();
			d6.setCourse( c3 );
			d6.setStudent( s3 );
			session.persist( d6 );
			final Demand d7 = new Demand();
			d7.setCourse( c4 );
			d7.setStudent( s1 );
			session.persist( d7 );
			final Demand d8 = new Demand();
			d8.setCourse( c5 );
			d8.setStudent( s2 );
			session.persist( d8 );
			final Demand d9 = new Demand();
			d9.setCourse( c6 );
			d9.setStudent( s2 );
			session.persist( d9 );
			final Demand d0 = new Demand();
			d0.setCourse( c6 );
			d0.setStudent( s3 );
			session.persist( d0 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Course" )
	static class Course {
		private UUID id;
		private String number;
		private Subject subject;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		@Column( name = "number_col" )
		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		@ManyToOne
		@JoinColumn( name = "subject_id" )
		public Subject getSubject() {
			return subject;
		}

		public void setSubject(Subject subject) {
			this.subject = subject;
		}

		@Override
		public String toString() {
			return getSubject() + " " + getNumber();
		}
	}

	@Entity( name = "Subject" )
	static class Subject {
		private UUID id;
		private String name;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@Entity( name = "Demand" )
	static class Demand {
		private UUID id;
		private Student student;
		private Course course;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn( name = "student_id" )
		public Student getStudent() {
			return student;
		}

		public void setStudent(Student student) {
			this.student = student;
		}

		@ManyToOne
		@JoinColumn( name = "course_id" )
		public Course getCourse() {
			return course;
		}

		public void setCourse(Course course) {
			this.course = course;
		}

		@Override
		public String toString() {
			return getStudent() + " for " + getCourse();
		}
	}

	@Entity( name = "Student" )
	static class Student {
		private UUID id;
		private String name;
		private Set<StudentMajor> majors;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany( fetch = FetchType.LAZY, mappedBy = "student", cascade = { CascadeType.ALL } )
		@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
		public Set<StudentMajor> getMajors() {
			return majors;
		}

		public void setMajors(Set<StudentMajor> majors) {
			this.majors = majors;
		}

		public void addToMajors(StudentMajor major) {
			if ( this.majors == null ) {
				this.majors = new HashSet<StudentMajor>();
			}
			this.majors.add( major );
		}

		@Override
		public String toString() {
			return getName() + " " + getMajors();
		}
	}

	@Entity( name = "StudentMajor" )
	static class StudentMajor {
		private UUID id;
		private Student student;
		private Major major;
		private String classification;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getClassification() {
			return classification;
		}

		public void setClassification(String classification) {
			this.classification = classification;
		}

		@ManyToOne
		@JoinColumn( name = "student_id" )
		public Student getStudent() {
			return student;
		}

		public void setStudent(Student student) {
			this.student = student;
		}

		@ManyToOne
		@JoinColumn( name = "major_id" )
		public Major getMajor() {
			return major;
		}

		public void setMajor(Major major) {
			this.major = major;
		}

		@Override
		public String toString() {
			return getMajor().getName() + " " + getClassification();
		}
	}

	@Entity( name = "Major" )
	static class Major {
		private UUID id;

		@Column( name = "name_col" )
		private String name;

		@Id
		@GeneratedValue
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
