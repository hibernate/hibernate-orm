package org.hibernate.test.dialect.functional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.lob.OracleSeqIdGenDialect;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13322" )
@RequiresDialect( Oracle8iDialect.class )
public class OracleExtractSequenceMetadataTest extends BaseCoreFunctionalTestCase {

	private static final String SEQUENCE_NAME = SequenceStyleGenerator.DEF_SEQUENCE_NAME;
	private static final String SEQUENCE_INCREMENT_SIZE = "50";

	private static final String OTHER_SCHEMA_NAME = "hibernate_orm_test_2";
	private static final String SEQUENCE_INCREMENT_SIZE_FROM_OTHER_SCHEMA = "1";

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( Environment.DIALECT, OracleSeqIdGenDialect.class.getName() );
		configuration.setProperty(
				AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
				SequenceMismatchStrategy.EXCEPTION.toString() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@BeforeClassOnce
	public static void setUpDB() throws Exception {

		assert ! SEQUENCE_INCREMENT_SIZE.equals( SEQUENCE_INCREMENT_SIZE_FROM_OTHER_SCHEMA );

		try ( Connection conn = getConnection() ) {
			try ( Statement stmt = conn.createStatement() ) {
				try {
					stmt.execute( String.format( "DROP USER %s CASCADE", OTHER_SCHEMA_NAME ) );
				}
				catch ( Exception ignore ) {
				}

				stmt.execute( String.format( "CREATE USER %s IDENTIFIED BY whatever", OTHER_SCHEMA_NAME ) );

				// create identical sequence in other schema with different 'increment size' than specified in id field of the entity
				stmt.execute( String.format( "CREATE SEQUENCE %s.%s START WITH 1 INCREMENT BY %s",
											 OTHER_SCHEMA_NAME,
											 SEQUENCE_NAME,
											 SEQUENCE_INCREMENT_SIZE_FROM_OTHER_SCHEMA
				) );

				// ensure no sequence exists for current schema, so the identical sequence in the other schema could be the only culprit
				try {
					stmt.execute( String.format(
							"DROP SEQUENCE %s",
							SEQUENCE_NAME
					) );
				}
				catch ( Exception ignore ) {
				}
			}
		}
	}

	@Test
	public void testHibernateLaunchSuccessfully() {
		// if we were lucky to reach here, woa, the sequence from other schema didn't bother us (with different 'increment size's)
	}

	@AfterClass
	public static void tearDownDB() throws SQLException {
		try ( Connection conn = getConnection() ) {
			try ( Statement stmt = conn.createStatement() ) {
				// dropping user with 'cascade' will drop the sequence as well
				stmt.execute( String.format( "DROP USER %s CASCADE", OTHER_SCHEMA_NAME ) );
			}
			catch ( Exception ignore ) {
			}
		}
	}

	private static Connection getConnection() throws SQLException {
		String url = Environment.getProperties().getProperty( Environment.URL );
		String user = Environment.getProperties().getProperty( Environment.USER );
		String password = Environment.getProperties().getProperty( Environment.PASS );
		return DriverManager.getConnection( url, user, password );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "sequence_generator" )
		@GenericGenerator(
				name = "sequence_generator",
				strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
				parameters = {
						@Parameter(name = "sequence_name", value = SEQUENCE_NAME),
						@Parameter(name = "initial_value", value = "1" ),
						@Parameter(name = "increment_size", value = SEQUENCE_INCREMENT_SIZE),
						@Parameter(name = "optimizer", value = "pooled")
				}
		)
		private Long id;

	}

}
