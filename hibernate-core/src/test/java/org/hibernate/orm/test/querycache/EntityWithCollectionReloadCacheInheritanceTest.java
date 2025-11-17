/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Miroslav Silhavy
 */
@DomainModel(annotatedClasses = {
		EntityWithCollectionReloadCacheInheritanceTest.HighSchoolStudent.class,
		EntityWithCollectionReloadCacheInheritanceTest.Subject.class,
		EntityWithCollectionReloadCacheInheritanceTest.EnglishSubject.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19387")
public class EntityWithCollectionReloadCacheInheritanceTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<HighSchoolStudent> highSchoolStudents = session.createQuery(
							"select s" +
									" from HighSchoolStudent s left join fetch s.subjects m" +
									" where s.name in :names", HighSchoolStudent.class
					)
					.setParameter( "names", Arrays.asList( "Brian" ) )
					.setCacheable( true )
					.list();

			assertThat( highSchoolStudents ).hasSize( 1 );

			highSchoolStudents = session.createQuery(
							"select s" +
									" from HighSchoolStudent s left join fetch s.subjects m" +
									" where s.name in :names", HighSchoolStudent.class
					)
					.setParameter( "names", Arrays.asList( "Andrew", "Brian" ) )
					.setCacheable( true )
					.list();

			assertThat( highSchoolStudents ).hasSize( 2 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HighSchoolStudent s1 = new HighSchoolStudent();
			s1.setName( "Andrew" );
			session.persist( s1 );

			HighSchoolStudent s2 = new HighSchoolStudent();
			s2.setName( "Brian" );
			session.persist( s2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "HighSchoolStudent")
	static class HighSchoolStudent {

		@Id
		@GeneratedValue
		@Column(name = "id")
		private Long id;

		@Column(name = "name")
		private String name;

		@ManyToMany(targetEntity = Subject.class, fetch = FetchType.LAZY)
		@JoinTable(name = "STUDENT_SUBJECT",
				joinColumns = { @JoinColumn(name = "student_id") },
				inverseJoinColumns = { @JoinColumn(name = "subject_id") }
		)
		private Set<Subject> subjects;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Subject> getSubjects() {
			return subjects;
		}

		public void setSubjects(Set<Subject> subjects) {
			this.subjects = subjects;
		}

	}

	@Entity(name = "Subject")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue("DEFAULT")
	@DiscriminatorColumn(name = "TYPE", length = 20)
	static class Subject {

		@Id
		@GeneratedValue
		@Column(name = "id")
		private Long id;

		@Column(name = "TYPE", nullable = false, length = 20, insertable = false, updatable = false)
		@Enumerated(STRING)
		@JdbcTypeCode(SqlTypes.VARCHAR)
		private SubjectType type;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "EnglishSubject")
	@DiscriminatorValue("ENGLISH")
	static class EnglishSubject extends Subject {
	}

	enum SubjectType {
		DEFAULT,
		ENGLISH
	}

}
