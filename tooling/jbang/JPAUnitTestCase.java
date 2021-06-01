/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.hibernate:hibernate-core:${hibernate-orm.version:5.5.0.Final}
//DEPS com.h2database:h2:1.4.200
//DEPS org.assertj:assertj-core:3.19.0
//DEPS junit:junit:4.13.2

// Testcontiainers dependencies
//DEPS org.testcontainers:postgresql:1.15.3
//DEPS org.testcontainers:mysql:1.15.3
//

// JDBC Drivers
//DEPS org.postgresql:postgresql:42.2.16
//DEPS mysql:mysql-connector-java:8.0.25
//

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static org.assertj.core.api.Assertions.assertThat;

public class JPAUnitTestCase {

	private static final Database DATABASE = Database.H2;

	private EntityManagerFactory factory;

	/*
	 * Create a new factory and a new schema before each test (see
	 * property `hibernate.hbm2ddl.auto`).
	 * This way each test will start with a clean database.
	 *
	 * The drawback is that, in a real case scenario with multiple tests,
	 * it can slow down the whole test suite considerably. If that happens,
	 * it's possible to make the session factory static and, if necessary,
	 * delete the content of the tables manually (without dropping them).
	 *
	 * Usually, one would create the entity manager via {@link Persistence#createEntityManager}.
	 * But it would require a persistence.xml. To keep everything in one file,
	 * we decided to create the factory programmatically.
	 */
	@Before
	public void createEntityManagerFactory() {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder()
				// Add in any settings that are specific to your test.
				.applySetting( AvailableSettings.URL, DATABASE.getJdbcUrl() )
				.applySetting( AvailableSettings.DIALECT, DATABASE.getDialect() )

				// Testcontainers takes care of the JDBC drivers
//				.applySetting( AvailableSettings.DRIVER, DATABASE.getDriver() )

				// (Optional) Override credentials
//				.applySetting( AvailableSettings.USER, "testuser" )
//				.applySetting( AvailableSettings.PASS, "testpass" )

				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.applySetting( AvailableSettings.SHOW_SQL, "true" )
				.applySetting( AvailableSettings.HIGHLIGHT_SQL, "true" )
				.applySetting( AvailableSettings.FORMAT_SQL, "true" );

		Metadata metadata = new MetadataSources( srb.build() )
				// Add your entities here.
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata();

		factory = metadata.buildSessionFactory();
	}


	@Test
	public void testInsertAndSelect() {
		// Create an entity
		MyEntity entity = new MyEntity( "Entity example", 23 );
		EntityManager entityManager = null;

		try {
			// Insert entity in the database
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist( entity );
			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}

		try {
			// Select entity from the database
			entityManager = factory.createEntityManager();
			MyEntity result = entityManager.find( MyEntity.class, entity.getId() );
			assertThat( result.getName() ).isEqualTo( entity.getName() );
		}
		finally {
			entityManager.close();
		}
	}

	@After
	public void closeFactory() {
		if ( factory != null ) {
			factory.close();
		}
	}

	/**
	 * Example of a class representing an entity.
	 * <p>
	 * If you create new entities, be sure to add them in {@link #createEntityManager()}.
	 * </p>
	 */
	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;

		public String name;

		public MyEntity() {
		}

		public MyEntity(String name, Integer id) {
			this.name = name;
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "MyEntity"
					+ "\n\t id = " + id
					+ "\n\t name = " + name;
		}
	}

	// Some already configured databases for convenience
	// Because we are using testcontainers, host and port value gets ignored
	// so there is no need to set them
	enum Database {
		H2( "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1",
			"org.hibernate.dialect.H2Dialect", "org.h2.Driver" ),

		POSTGRESQL( "jdbc:tc:postgresql:9.6.8:///testdb?user=testuser&password=testpass",
					"org.hibernate.dialect.PostgreSQL10Dialect", "org.postgresql.Driver" ),

		MYSQL( "jdbc:tc:mysql:5.7.34:///testdb?user=testuser&password=testpass",
			   "org.hibernate.dialect.MySQLDialect", "com.mysql.jdbc.Driver" );

		private final String jdbcUrl;
		private final String driver;
		private final String dialect;

		Database(String jdbcUrl, String dialect, String driver) {
			this.dialect = dialect;
			this.driver = driver;
			this.jdbcUrl = jdbcUrl;
		}

		public String getDialect() {
			return dialect;
		}

		public String getDriver() {
			return driver;
		}

		public String getJdbcUrl() {
			return jdbcUrl;
		}
	}

	// This main class is only for JBang so that it can run the tests with `jbang JPAUnitTestCase`
	public static void main(String[] args) {
		System.out.println( "Starting the test suite" );

		Result result = JUnitCore.runClasses( JPAUnitTestCase.class );

		for ( Failure failure : result.getFailures() ) {
			System.out.println();
			System.err.println( "Test " + failure.getTestHeader() + " FAILED!" );
			System.err.println( "\t" + failure.getTrace() );
		}

		System.out.println();
		System.out.print( "Tests result summary: " );
		System.out.println( result.wasSuccessful() ? "SUCCESS" : "FAILURE" );
	}
}
