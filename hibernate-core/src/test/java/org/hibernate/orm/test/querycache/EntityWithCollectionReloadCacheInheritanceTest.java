/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import static jakarta.persistence.AccessType.FIELD;
import static jakarta.persistence.EnumType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author miroslav silhavy
 */
@DomainModel(annotatedClasses = {
		EntityWithCollectionReloadCacheInheritanceTest.HighSchoolStudent.class,
		EntityWithCollectionReloadCacheInheritanceTest.DefaultSubject.class,
		EntityWithCollectionReloadCacheInheritanceTest.EnglishSubject.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19387")
public class EntityWithCollectionReloadCacheInheritanceTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<HighSchoolStudent> list = session.createQuery(
							"select s" +
									" from HighSchoolStudent s left join fetch s.subjects m" +
									" where s.name in :names", HighSchoolStudent.class
					)
					.setParameter( "names", Arrays.asList( "Brian" ) )
					.setCacheable( true )
					.list();

			assertThat( list ).hasSize( 1 );

			list = session.createQuery(
							"select s" +
									" from HighSchoolStudent s left join fetch s.subjects m" +
									" where s.name in :names", HighSchoolStudent.class
					)
					.setParameter( "names", Arrays.asList( "Andrew", "Brian" ) )
					.setCacheable( true )
					.list();

			assertThat( list ).hasSize( 2 );
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
	@Access(FIELD)
	static class HighSchoolStudent {

		@Id
		@GeneratedValue
		@Column(name = "id")
		private Long id;

		@Column(name = "name")
		private String name;

		@ManyToMany(targetEntity = DefaultSubject.class, fetch = FetchType.LAZY)
		@JoinTable(name = "STUDENT_SUBJECT",
				joinColumns = { @JoinColumn(name = "student_id") },
				inverseJoinColumns = { @JoinColumn(name = "subject_id") }
		)
		private Set<DefaultSubject> subjects;

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

		public Set<DefaultSubject> getSubjects() {
			return subjects;
		}

		public void setMajors(Set<DefaultSubject> subjects) {
			this.subjects = subjects;
		}

	}

	@Entity(name = "DefaultSubject")
	@DiscriminatorValue("DEFAULT")
	@DiscriminatorColumn(name = "TYPE", length = 20)
	@Access(FIELD)
	static class DefaultSubject {

		enum SubjectType {
			DEFAULT,
			ENGLISH
		}

		@Column(name = "TYPE", nullable = false, length = 20, insertable = false, updatable = false)
		@Enumerated(STRING)
		@JdbcTypeCode(SqlTypes.VARCHAR)
		private SubjectType type;

		@Id
		@GeneratedValue
		@Column(name = "id")
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "EnglishSubject")
	@DiscriminatorValue("ENGLISH")
	@Access(FIELD)
	static class EnglishSubject extends DefaultSubject {
	}

}
