/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@RequiresDialect(value = MariaDBDialect.class)
public class MariaDBExtractSequenceMetadataTest {

	private static String primaryDbName;
	private static String primarySequenceName = "seq_HHH13373";

	private static String secondaryDbName = "secondary_db_HHH13373";
	private static String secondarySequenceName = "secondary_seq_HHH13373";

	@BeforeAll
	public static void setUpDBs() throws Exception {
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet resultSet = stmt.executeQuery( "SELECT DATABASE()" )) {
					assert resultSet.next();
					primaryDbName = resultSet.getString( 1 );
				}
				stmt.execute( "CREATE DATABASE IF NOT EXISTS " + secondaryDbName );
				stmt.execute( "USE " + secondaryDbName );
				stmt.execute( "CREATE SEQUENCE IF NOT EXISTS " + secondarySequenceName );
				stmt.execute( "USE " + primaryDbName );
				stmt.execute( "DROP SEQUENCE IF EXISTS " + secondarySequenceName );
				stmt.execute( "CREATE SEQUENCE IF NOT EXISTS " + primarySequenceName );
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-13373")
	public void testHibernateLaunchedSuccessfully() {
		JdbcEnvironment jdbcEnvironment = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() )
				.getService( JdbcEnvironment.class );
		Assertions.assertFalse( jdbcEnvironment.getExtractedDatabaseMetaData().getSequenceInformationList().isEmpty() );
	}

	@AfterAll
	public static void tearDownDBs() throws SQLException {
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute( "DROP DATABASE IF EXISTS " + secondaryDbName );
			}
			catch (Exception e) {
				// Ignore
			}

			try (Statement stmt = conn.createStatement()) {
				stmt.execute( "DROP SEQUENCE IF EXISTS " + primarySequenceName );
			}
			catch (Exception e) {
				// Ignore
			}
		}
	}

	private static Connection getConnection() throws SQLException {
		String url = Environment.getProperties().getProperty( Environment.URL );
		String user = Environment.getProperties().getProperty( Environment.USER );
		String password = Environment.getProperties().getProperty( Environment.PASS );
		return DriverManager.getConnection( url, user, password );
	}

}
