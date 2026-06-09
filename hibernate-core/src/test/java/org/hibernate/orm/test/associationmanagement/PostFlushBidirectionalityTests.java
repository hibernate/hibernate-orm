/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		Parent.class,
		Child.class,
		Person.class,
		Passport.class,
		Student.class,
		Course.class
} )
@SessionFactory( useCollectingStatementObserver = true )
@ServiceRegistry( settings = @Setting(
		name = AvailableSettings.BIDIRECTIONALITY_MANAGEMENT,
		value = "true"
) )
class PostFlushBidirectionalityTests {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void manyToOneReassignmentRepairsOldAndNewParentCollections(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent oldParent = new Parent( 1 );
			final Parent newParent = new Parent( 2 );
			final Child child = new Child( 1, oldParent );
			oldParent.children.add( child );
			session.persist( oldParent );
			session.persist( newParent );
			session.persist( child );
		} );

		final var sqlCollector = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Parent oldParent = session.get( Parent.class, 1 );
			final Parent newParent = session.get( Parent.class, 2 );
			final Child child = session.get( Child.class, 1 );

			// initialize the 2 collections manually
			oldParent.children.size();
			newParent.children.size();

			// reassign parent and flush, should
			//		- issue the update
			//		- adjust old and new Parent#children since both are loaded
			child.parent = newParent;
			session.flush();

			sqlCollector.clear();
			assertThat( oldParent.children ).doesNotContain( child );
			assertThat( newParent.children ).containsExactly( child );
			session.flush();
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
		} );
	}

	@Test
	void owningOneToOneReassignmentRepairsInverseReferences(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person oldPerson = new Person( 1 );
			final Person newPerson = new Person( 2 );
			final Passport passport = new Passport( 1, oldPerson );
			oldPerson.passport = passport;
			session.persist( oldPerson );
			session.persist( newPerson );
			session.persist( passport );
		} );

		scope.inTransaction( session -> {
			final Person oldPerson = session.get( Person.class, 1 );
			final Person newPerson = session.get( Person.class, 2 );
			final Passport passport = session.get( Passport.class, 1 );

			passport.setPerson( newPerson );
			session.flush();

			assertThat( oldPerson.passport ).isNull();
			assertThat( newPerson.passport ).isSameAs( passport );
		} );
	}

	@Test
	void owningManyToManyCollectionRepairsInitializedInverseCollections(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Student student = new Student( 1 );
			final Course oldCourse = new Course( 1 );
			final Course newCourse = new Course( 2 );
			student.courses.add( oldCourse );
			oldCourse.students.add( student );
			session.persist( student );
			session.persist( oldCourse );
			session.persist( newCourse );
		} );

		final var sqlCollector = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Student student = session.get( Student.class, 1 );
			final Course oldCourse = session.get( Course.class, 1 );
			final Course newCourse = session.get( Course.class, 2 );
			student.courses.size();
			oldCourse.students.size();
			newCourse.students.size();

			student.courses.remove( oldCourse );
			student.courses.add( newCourse );
			session.flush();

			assertThat( oldCourse.students ).doesNotContain( student );
			assertThat( newCourse.students ).containsExactly( student );

			sqlCollector.clear();
			session.flush();
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
		} );
	}

}
