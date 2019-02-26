/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-13228" )
public class OneToOneMapsIdChangeParentTest extends EntityManagerFactoryBasedFunctionalTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(
					CoreMessageLogger.class,
					EntityTypeDescriptor.class.getName()
			)
	);

	private Triggerable triggerable = logInspection.watchForLogMessages( "HHH000502:" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	@Disabled("Enable once updates work properly")
	public void test() {
		Child _child = doInJPA( this::entityManagerFactory, entityManager -> {
			Parent firstParent = new Parent();
			firstParent.setId( 1L );
			entityManager.persist(firstParent);

			Child child = new Child();
			child.setParent(firstParent);
			entityManager.persist( child );

			return child;
		} );

		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent secondParent = new Parent();
			secondParent.setId( 2L );
			entityManager.persist(secondParent);

			_child.setParent(secondParent);

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
