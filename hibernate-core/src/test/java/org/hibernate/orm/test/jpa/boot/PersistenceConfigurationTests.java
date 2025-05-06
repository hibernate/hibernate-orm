/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.env.TestingDatabaseInfo;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.domain.library.Person;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;

import static jakarta.persistence.PersistenceConfiguration.JDBC_PASSWORD;
import static jakarta.persistence.PersistenceConfiguration.JDBC_URL;
import static jakarta.persistence.PersistenceConfiguration.JDBC_USER;

/**
 * Simple tests for {@linkplain PersistenceConfiguration} and {@linkplain HibernatePersistenceConfiguration}.
 *
 * @author Steve Ebersole
 */
public class PersistenceConfigurationTests {
	@Test
	@RequiresDialect( H2Dialect.class )
	void testBaseJpa() {
		try (EntityManagerFactory emf = new PersistenceConfiguration( "emf" ).createEntityManagerFactory()) {
			assert emf.isOpen();
		}
		try (EntityManagerFactory emf = new HibernatePersistenceConfiguration( "emf" ).createEntityManagerFactory()) {
			assert emf.isOpen();
		}
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	void testCallPersistence() {
		final PersistenceConfiguration cfg1 = new PersistenceConfiguration( "emf" );
		try (EntityManagerFactory emf = Persistence.createEntityManagerFactory( cfg1 )) {
			assert emf.isOpen();
		}

		final HibernatePersistenceConfiguration cfg2 = new HibernatePersistenceConfiguration( "emf" );
		try (EntityManagerFactory emf = Persistence.createEntityManagerFactory( cfg2 )) {
			assert emf.isOpen();
		}
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	void testJdbcData() {
		final PersistenceConfiguration cfg = new PersistenceConfiguration( "emf" );
		TestingDatabaseInfo.forEachSetting( cfg::property );
		try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
			assert emf.isOpen();
		}

		final PersistenceConfiguration cfg2 = new PersistenceConfiguration( "emf" );
		TestingDatabaseInfo.forEachSetting( cfg2::property );
		try (EntityManagerFactory emf = cfg2.createEntityManagerFactory()) {
			assert emf.isOpen();
		}

		final PersistenceConfiguration cfg3 = new HibernatePersistenceConfiguration( "emf" );
		TestingDatabaseInfo.forEachSetting( cfg3::property );
		try (EntityManagerFactory emf = cfg3.createEntityManagerFactory()) {
			assert emf.isOpen();
		}

		final PersistenceConfiguration cfg4 = new HibernatePersistenceConfiguration( "emf" )
				.jdbcDriver( TestingDatabaseInfo.DRIVER )
				.jdbcUrl( TestingDatabaseInfo.URL )
				.jdbcUsername( TestingDatabaseInfo.USER )
				.jdbcPassword( TestingDatabaseInfo.PASS );
		try (EntityManagerFactory emf = cfg4.createEntityManagerFactory()) {
			assert emf.isOpen();
		}
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	public void testForUserGuide() {
		{
			//tag::example-bootstrap-standard-PersistenceConfiguration[]
			final PersistenceConfiguration cfg = new PersistenceConfiguration( "emf" )
					.property( JDBC_URL, "jdbc:h2:mem:db1" )
					.property( JDBC_USER, "sa" )
					.property( JDBC_PASSWORD, "" );
			try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
				assert emf.isOpen();
			}
			//end::example-bootstrap-standard-PersistenceConfiguration[]
		}

		{
			//tag::example-bootstrap-standard-HibernatePersistenceConfiguration[]
			final PersistenceConfiguration cfg = new HibernatePersistenceConfiguration( "emf" )
					.jdbcUrl( "jdbc:h2:mem:db1" )
					.jdbcUsername( "sa" )
					.jdbcPassword( "" );
			try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
				assert emf.isOpen();
			}
			//end::example-bootstrap-standard-HibernatePersistenceConfiguration[]
		}
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	public void testVarargs() {
		final PersistenceConfiguration cfg = new HibernatePersistenceConfiguration( "emf" )
				.jdbcUrl( "jdbc:h2:mem:db1" )
				.jdbcUsername( "sa" )
				.jdbcPassword( "" )
				.schemaToolingAction( Action.CREATE_DROP )
				.managedClasses( Book.class, Person.class );
		try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
			assert emf.isOpen();
			TransactionUtil2.inTransaction( emf.unwrap( SessionFactoryImplementor.class ), (em) -> {
				em.createSelectionQuery( "from Book", Book.class ).list();
			} );
		}
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	public void testVarargs2() {
		final PersistenceConfiguration cfg = new HibernatePersistenceConfiguration( "emf" )
				.jdbcUrl( "jdbc:h2:mem:db1" )
				.jdbcUsername( "sa" )
				.jdbcPassword( "" )
				.schemaToolingAction( Action.CREATE_DROP )
				.managedClasses( List.of( Book.class, Person.class ) );
		try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
			assert emf.isOpen();
			TransactionUtil2.inTransaction( emf.unwrap( SessionFactoryImplementor.class ), (em) -> {
				em.createSelectionQuery( "from Book", Book.class ).list();
			} );
		}
	}
}
