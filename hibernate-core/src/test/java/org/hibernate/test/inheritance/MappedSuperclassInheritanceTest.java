/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12653")
public class MappedSuperclassInheritanceTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger( CoreMessageLogger.class, AnnotationBinder.class.getName() ) );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
				Manager.class,
				Developer.class
		};
	}

	@Override
	public void buildEntityManagerFactory() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000503:" );
		triggerable.reset();
		assertFalse( triggerable.wasTriggered() );

		super.buildEntityManagerFactory();

		assertTrue( triggerable.wasTriggered() );
		assertTrue( triggerable.triggerMessage().contains( "A class should not be annotated with both @Inheritance and @MappedSuperclass. @Inheritance will be ignored for" ) );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery("from Manager").getResultList();
			entityManager.createQuery("from Developer").getResultList();

			try {
				//Check the @Inheritance annotation was ignored
				entityManager.createQuery("from Employee").getResultList();
				fail();
			} catch (Exception expected) {
				QuerySyntaxException rootException = (QuerySyntaxException) ExceptionUtil.rootCause(expected);
				assertEquals("Employee is not mapped", rootException.getMessage());
			}
		} );
	}

	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@MappedSuperclass
	public static class Employee {

		@Id
		@GeneratedValue
		private Long id;

		private String jobType;

		private String firstName;

		private String lastName;
	}

	@Entity(name = "Manager")
	public static class Manager extends Employee {
	}

	@Entity(name = "Developer")
	public static class Developer extends Employee {
	}
}
