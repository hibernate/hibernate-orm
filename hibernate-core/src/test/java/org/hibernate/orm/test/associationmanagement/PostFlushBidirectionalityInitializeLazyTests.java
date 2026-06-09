/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */

@DomainModel( annotatedClasses = {
		Parent.class,
		Child.class,
		Person.class,
		Passport.class,
		Student.class,
		Course.class
} )
@SessionFactory( useCollectingStatementObserver = true )
@ServiceRegistry( settings = {
		@Setting(
				name = AvailableSettings.BIDIRECTIONALITY_MANAGEMENT,
				value = "true"
		),
		@Setting(
				name = AvailableSettings.BIDIRECTIONALITY_MANAGEMENT_LAZY_POLICY,
				value = "initialize"
		)
} )
public class PostFlushBidirectionalityInitializeLazyTests {

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
		sqlCollector.clear();

		scope.inTransaction( session -> {
			final Parent newParent = session.get( Parent.class, 2 );
			final Child child = session.get( Child.class, 1 );

			assertThat( Hibernate.isInitialized( newParent.children ) ).isFalse();

			// load of child + load of newParent
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			sqlCollector.clear();

			// Note that we loaded newParent but that its children collection is still uninitialized

			// reassign parent and flush, should
			//		- issue the update
			//		- bidirectionality management
			//			- initialize newParent#children
			//			- adjust newParent#children
			child.parent = newParent;
			session.flush();

			// update + load of newParent#children
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( newParent.children ) ).isTrue();

			assertThat( newParent.children ).containsExactly( child );

			// make sure the bidirectionality management does not trigger any writes on flush
			sqlCollector.clear();
			session.flush();
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
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

			assertThat( Hibernate.isInitialized( student.courses ) ).isFalse();
			assertThat( Hibernate.isInitialized( oldCourse.students ) ).isFalse();
			assertThat( Hibernate.isInitialized( newCourse.students ) ).isFalse();

			Hibernate.initialize( student.courses );

			student.courses.remove( oldCourse );
			student.courses.add( newCourse );
			session.flush();

			assertThat( Hibernate.isInitialized( student.courses ) ).isTrue();
			assertThat( Hibernate.isInitialized( oldCourse.students ) ).isTrue();
			assertThat( Hibernate.isInitialized( newCourse.students ) ).isTrue();

			assertThat( oldCourse.students ).doesNotContain( student );
			assertThat( newCourse.students ).containsExactly( student );

			sqlCollector.clear();
			session.flush();
			assertThat( sqlCollector.getSqlQueries() ).isEmpty();
		} );
	}

}
