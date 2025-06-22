/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = StatelessSessionConnectionTest.Employee.class,
		properties = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10")
)
public class StatelessSessionConnectionTest {

	final CoreMessageLogger messageLogger = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			SequenceStyleGenerator.class.getName()
	);

	@RegisterExtension
	public LoggerInspectionExtension logInspection = LoggerInspectionExtension
			.builder().setLogger( messageLogger ).build();


	@Test
	@JiraKey(value = "HHH-11732")
	public void test(EntityManagerFactoryScope scope) {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000352" );
		triggerable.reset();

		StatelessSession session = scope.getEntityManagerFactory()
				.unwrap( SessionFactory.class )
				.openStatelessSession();
		Transaction tx = session.beginTransaction();

		try {
			Employee employee = new Employee( "1", "2", 1 );
			employee.setId( 1 );
			session.insert( employee );

			tx.rollback();
		}
		catch (HibernateException e) {
			if ( tx != null ) {
				tx.rollback();
			}
		}
		finally {
			session.close();
			assertFalse( triggerable.wasTriggered() );
		}
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private Integer id;

		private String firstName;

		private String lastName;

		private int salary;

		public Employee() {
		}

		public Employee(String fname, String lname, int salary) {
			this.firstName = fname;
			this.lastName = lname;
			this.salary = salary;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String first_name) {
			this.firstName = first_name;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String last_name) {
			this.lastName = last_name;
		}

		public int getSalary() {
			return salary;
		}

		public void setSalary(int salary) {
			this.salary = salary;
		}
	}
}
