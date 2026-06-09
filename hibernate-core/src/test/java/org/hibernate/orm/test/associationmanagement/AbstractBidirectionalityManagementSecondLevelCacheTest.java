/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractBidirectionalityManagementSecondLevelCacheTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAll();
	}

	@Test
	void oneToManyManagementParticipatesInCollectionCacheInvalidation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CachedParent oldParent = new CachedParent( 1 );
			final CachedParent newParent = new CachedParent( 2 );
			final CachedChild child = new CachedChild( 1, oldParent );
			oldParent.children.add( child );
			session.persist( oldParent );
			session.persist( newParent );
			session.persist( child );
		} );

		scope.inSession( session -> {
			assertThat( session.get( CachedParent.class, 1 ).children ).containsExactly( session.get( CachedChild.class, 1 ) );
			assertThat( session.get( CachedParent.class, 2 ).children ).isEmpty();
		} );

		scope.inTransaction( session -> {
			final CachedParent oldParent = session.get( CachedParent.class, 1 );
			final CachedParent newParent = session.get( CachedParent.class, 2 );
			final CachedChild child = session.get( CachedChild.class, 1 );
			oldParent.children.size();
			newParent.children.size();

			child.parent = newParent;
			session.flush();

			assertThat( oldParent.children ).doesNotContain( child );
			assertThat( newParent.children ).containsExactly( child );
		} );

		scope.inSession( session -> assertOneToManyStateInLaterSession(
				session.get( CachedParent.class, 1 ),
				session.get( CachedParent.class, 2 ),
				session.get( CachedChild.class, 1 )
		) );
	}

	@Test
	void manyToManyManagementParticipatesInCollectionCacheInvalidation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CachedStudent student = new CachedStudent( 1 );
			final CachedCourse oldCourse = new CachedCourse( 1 );
			final CachedCourse newCourse = new CachedCourse( 2 );
			student.courses.add( oldCourse );
			oldCourse.students.add( student );
			session.persist( student );
			session.persist( oldCourse );
			session.persist( newCourse );
		} );

		scope.inSession( session -> {
			assertThat( session.get( CachedStudent.class, 1 ).courses ).containsExactly( session.get( CachedCourse.class, 1 ) );
			assertThat( session.get( CachedCourse.class, 1 ).students ).containsExactly( session.get( CachedStudent.class, 1 ) );
			assertThat( session.get( CachedCourse.class, 2 ).students ).isEmpty();
		} );

		scope.inTransaction( session -> {
			final CachedStudent student = session.get( CachedStudent.class, 1 );
			final CachedCourse oldCourse = session.get( CachedCourse.class, 1 );
			final CachedCourse newCourse = session.get( CachedCourse.class, 2 );
			student.courses.size();
			oldCourse.students.size();
			newCourse.students.size();

			student.courses.remove( oldCourse );
			student.courses.add( newCourse );
			session.flush();

			assertThat( oldCourse.students ).doesNotContain( student );
			assertThat( newCourse.students ).containsExactly( student );
		} );

		scope.inSession( session -> assertManyToManyStateInLaterSession(
				session.get( CachedCourse.class, 1 ),
				session.get( CachedCourse.class, 2 ),
				session.get( CachedStudent.class, 1 )
		) );
	}

	@Test
	void oneToOneManagementParticipatesInEntityCacheInvalidation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CachedPerson oldPerson = new CachedPerson( 1 );
			final CachedPerson newPerson = new CachedPerson( 2 );
			final CachedPassport passport = new CachedPassport( 1, oldPerson );
			oldPerson.passport = passport;
			session.persist( oldPerson );
			session.persist( newPerson );
			session.persist( passport );
		} );

		scope.inSession( session -> {
			assertThat( session.get( CachedPerson.class, 1 ).passport ).isSameAs( session.get( CachedPassport.class, 1 ) );
			assertThat( session.get( CachedPerson.class, 2 ).passport ).isNull();
		} );

		scope.inTransaction( session -> {
			final CachedPerson oldPerson = session.get( CachedPerson.class, 1 );
			final CachedPerson newPerson = session.get( CachedPerson.class, 2 );
			final CachedPassport passport = session.get( CachedPassport.class, 1 );

			passport.person = newPerson;
			session.flush();

			assertThat( oldPerson.passport ).isNull();
			assertThat( newPerson.passport ).isSameAs( passport );
		} );

		scope.inSession( session -> {
			assertThat( session.get( CachedPerson.class, 1 ).passport ).isNull();
			assertThat( session.get( CachedPerson.class, 2 ).passport ).isSameAs( session.get( CachedPassport.class, 1 ) );
		} );
	}

	void assertOneToManyStateInLaterSession(CachedParent oldParent, CachedParent newParent, CachedChild child) {
		assertThat( oldParent.children ).doesNotContain( child );
		assertThat( newParent.children ).containsExactly( child );
	}

	void assertManyToManyStateInLaterSession(CachedCourse oldCourse, CachedCourse newCourse, CachedStudent student) {
		assertThat( oldCourse.students ).doesNotContain( student );
		assertThat( newCourse.students ).containsExactly( student );
	}

	@Entity(name = "CachedParent")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedParent {
		@Id
		Integer id;

		@OneToMany(mappedBy = "parent")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Set<CachedChild> children = new HashSet<>();

		CachedParent() {
		}

		CachedParent(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "CachedChild")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedChild {
		@Id
		Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		CachedParent parent;

		CachedChild() {
		}

		CachedChild(Integer id, CachedParent parent) {
			this.id = id;
			this.parent = parent;
		}
	}

	@Entity(name = "CachedStudent")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedStudent {
		@Id
		Integer id;

		@ManyToMany
		@JoinTable(name = "cached_student_course")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Set<CachedCourse> courses = new HashSet<>();

		CachedStudent() {
		}

		CachedStudent(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "CachedCourse")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedCourse {
		@Id
		Integer id;

		@ManyToMany(mappedBy = "courses")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Set<CachedStudent> students = new HashSet<>();

		CachedCourse() {
		}

		CachedCourse(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "CachedPerson")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedPerson {
		@Id
		Integer id;

		@OneToOne(mappedBy = "person")
		CachedPassport passport;

		CachedPerson() {
		}

		CachedPerson(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "CachedPassport")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CachedPassport {
		@Id
		Integer id;

		@OneToOne(fetch = FetchType.LAZY)
		CachedPerson person;

		CachedPassport() {
		}

		CachedPassport(Integer id, CachedPerson person) {
			this.id = id;
			this.person = person;
		}
	}
}
