package org.hibernate.jpa.test.schemagen;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

public class JpaSchemaGeneratorTest extends BaseEntityManagerFunctionalTestCase {

	private static int schemagenNumber = 0;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlLoadScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE,
				"org/hibernate/jpa/test/schemagen/load-script-source.sql" );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlLoadScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE,
				getClass().getClassLoader().getResource( "org/hibernate/jpa/test/schemagen/load-script-source.sql" )
						.toString() );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE,
				"org/hibernate/jpa/test/schemagen/create-script-source.sql" );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE,
				getClass().getClassLoader().getResource( "org/hibernate/jpa/test/schemagen/create-script-source.sql" )
						.toString() );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE,
				"org/hibernate/jpa/test/schemagen/drop-script-source.sql" );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE,
				getClass().getClassLoader().getResource( "org/hibernate/jpa/test/schemagen/drop-script-source.sql" )
						.toString() );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	private void doTest(Map settings) {
		// We want a fresh db after emf close
		// Unfortunately we have to use this dirty hack because the db seems not to be closed otherwise
		settings.put( "hibernate.connection.url", "jdbc:h2:mem:db-schemagen" + schemagenNumber++
				+ ";MVCC=TRUE;LOCK_TIMEOUT=10000" );
		EntityManagerFactoryBuilder emfb = Bootstrap.getEntityManagerFactoryBuilder( buildPersistenceUnitDescriptor(),
				settings );

		EntityManagerFactory emf = emfb.build();

		Assert.assertNotNull( emf.createEntityManager().find( Item.class, "schemagen-test" ) );

		emf.close();
		emfb.cancel();
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	/* Disable hibernate schema export */
	@Override
	protected boolean createSchema() {
		return false;
	}

}
