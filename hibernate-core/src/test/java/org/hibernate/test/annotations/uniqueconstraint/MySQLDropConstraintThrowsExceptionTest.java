/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11236")
@RequiresDialect(MySQL5InnoDBDialect.class)
public class MySQLDropConstraintThrowsExceptionTest extends BaseUnitTestCase {

	@Test
	@FailureExpected(jiraKey = "HHH-11236")
	public void testEnumTypeInterpretation() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "drop" )
				.build();

		SessionFactoryImplementor sessionFactory = null;

		try {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "update" )
				.applySetting( AvailableSettings.HBM2DDL_HALT_ON_ERROR, Boolean.TRUE )
				.build();

		try {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Entity
	@Table(name = "CUSTOMER")
	public static class Customer {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic
		@Column(name = "CUSTOMER_ID", unique = true)
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public Customer() {
		}
	}
}
