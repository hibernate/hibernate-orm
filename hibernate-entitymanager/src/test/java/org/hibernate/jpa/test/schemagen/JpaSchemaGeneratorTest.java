/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import java.net.URL;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * Basic tests for JPA 2.1 schema export
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorTest extends BaseEntityManagerFunctionalTestCase {
	private static final String LOAD_SQL = "org/hibernate/jpa/test/schemagen/load-script-source.sql";
	private static final String CREATE_SQL = "org/hibernate/jpa/test/schemagen/create-script-source.sql";
	private static final String DROP_SQL = "org/hibernate/jpa/test/schemagen/drop-script-source.sql";

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
		settings.put( AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE, LOAD_SQL );
		doTest( settings );
	}


	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlLoadScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE, getResourceUrlString( LOAD_SQL ) );
		doTest( settings );
	}

	private String getResourceUrlString(String resource) {
		final URL url = getClass().getClassLoader().getResource( resource );
		if ( url == null ) {
			throw new RuntimeException( "Unable to locate requested resource [" + resource + "]" );
		}
		return url.toString();
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE, CREATE_SQL );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE, getResourceUrlString( CREATE_SQL ) );
		doTest( settings );
	}


	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE, DROP_SQL );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE, getResourceUrlString( DROP_SQL ) );
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