/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11236")
@RequiresDialect(value = MySQLDialect.class, version = 500)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@BaseUnitTest
public class MySQLDropConstraintThrowsExceptionTest {

	@BeforeEach
	public void setUp() {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
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
	}

	@AfterEach
	public void tearDown() {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
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

	}

	@Test
	public void testEnumTypeInterpretation() {
		final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
				false,
				false
		);

		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "update" )
				.applySetting(
						AvailableSettings.CONNECTION_PROVIDER,
						connectionProvider
				)
				.build();

		SessionFactory sessionFactory = null;
		try {
			final Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();
			sessionFactory = metadata.buildSessionFactory();
			List<String> alterStatements = connectionProvider.getExecuteStatements().stream()
					.filter(
							sql -> sql.toLowerCase().contains( "alter " )
					).map( String::trim ).collect( Collectors.toList() );
			if ( metadata.getDatabase().getDialect() instanceof MariaDBDialect ) {
				assertTrue( alterStatements.get( 0 ).matches( "alter table if exists CUSTOMER\\s+drop index .*?" ) );
				assertTrue( alterStatements.get( 1 )
									.matches( "alter table if exists CUSTOMER\\s+add constraint .*? unique \\(CUSTOMER_ID\\)" ) );

			}
			else {
				assertTrue( alterStatements.get( 0 ).matches( "alter table CUSTOMER\\s+drop index .*?" ) );
				assertTrue( alterStatements.get( 1 )
									.matches( "alter table CUSTOMER\\s+add constraint .*? unique \\(CUSTOMER_ID\\)" ) );
			}
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
