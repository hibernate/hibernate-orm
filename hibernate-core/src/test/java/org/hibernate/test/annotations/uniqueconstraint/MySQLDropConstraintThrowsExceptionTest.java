/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.uniqueconstraint;

import java.util.List;
import java.util.stream.Collectors;
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

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11236")
@RequiresDialect(MySQL5InnoDBDialect.class)
public class MySQLDropConstraintThrowsExceptionTest extends BaseUnitTestCase {

	@After
	public void releaseResources() {

	}

	@Test
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

		PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

		serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "update" )
				.applySetting(
						AvailableSettings.CONNECTION_PROVIDER,
						connectionProvider
				)
				.build();

		try {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
			List<String> alterStatements = connectionProvider.getExecuteStatements().stream()
			.filter(
				sql -> sql.toLowerCase().contains( "alter " )
			).map( String::trim ).collect( Collectors.toList() );
			assertTrue(alterStatements.get(0).matches( "alter table CUSTOMER\\s+drop index .*?" ));
			assertTrue(alterStatements.get(1).matches( "alter table CUSTOMER\\s+add constraint .*? unique \\(CUSTOMER_ID\\)" ));
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
