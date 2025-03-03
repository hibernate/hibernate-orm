/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13228")
@Jpa(
		annotatedClasses = {
				OneToOneMapsIdChangeParentTest.Parent.class,
				OneToOneMapsIdChangeParentTest.Child.class
		}
)
public class OneToOneMapsIdChangeParentTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(
					MethodHandles.lookup(),
					CoreMessageLogger.class,
					UpdateCoordinatorStandard.class.getName()
			)
	);

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000502:" );


	@Test
	public void test(EntityManagerFactoryScope scope) {
		Child _child = scope.fromTransaction( entityManager -> {
			Parent firstParent = new Parent();
			firstParent.setId( 1L );
			entityManager.persist( firstParent );

			Child child = new Child();
			child.setParent( firstParent );
			entityManager.persist( child );

			return child;
		} );

		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );

		scope.inTransaction( entityManager -> {
			Parent secondParent = new Parent();
			secondParent.setId( 2L );
			entityManager.persist( secondParent );

			_child.setParent( secondParent );

			entityManager.merge( _child );
		} );

		assertTrue( triggerable.wasTriggered() );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		@OneToOne
		@MapsId
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

	}

}
