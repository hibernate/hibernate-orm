/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import java.net.URL;
import java.util.Map;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.test.legacy.S;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorTest extends BaseEntityManagerFunctionalTestCase {

	private final String LOAD_SQL = getScriptFolderPath() + "load-script-source.sql , " + getScriptFolderPath() + "load-script-source2.sql";
	private final String CREATE_SQL = getScriptFolderPath() + "create-script-source.sql , " + getScriptFolderPath() + "create-script-source2.sql";
	private final String DROP_SQL = getScriptFolderPath() + "drop-script-source.sql , " + getScriptFolderPath() + "drop-script-source2.sql";

	private static int schemagenNumber = 0;

	public String getScriptFolderPath() {
		return "org/hibernate/jpa/test/schemagen/";
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlLoadScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, getLoadSqlScript() );
		doTest( settings );
	}


	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlLoadScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, getResourceUrlString( getLoadSqlScript() ) );
		doTest( settings );
	}

	protected String getResourceUrlString(String string) {
		return getResourceUrlString( getClass().getClassLoader(), string, URL::toString );
	}

	protected String getResourceUrlString(ClassLoader classLoader, String string, Function<URL, String> transformer) {
		final String[] strings = string.split( "\\s*,\\s*" );
		final StringBuilder sb = new StringBuilder( string.length() );
		for ( int i = 0; i < strings.length; i++ ) {
			if ( i != 0 ) {
				sb.append( ',' );
			}
			final String resource = strings[i];
			final URL url = classLoader.getResource( resource );
			if ( url == null ) {
				throw new RuntimeException( "Unable to locate requested resource [" + resource + "]" );
			}
			sb.append( transformer.apply( url ) );
		}
		return sb.toString();
	}

	protected String toFilePath(String relativePath) {
		return getResourceUrlString( Thread.currentThread().getContextClassLoader(), relativePath, URL::getFile );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE, getCreateSqlScript() );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlCreateScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE, getResourceUrlString( getCreateSqlScript() ) );
		doTest( settings );
	}


	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceClasspath() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE, getDropSqlScript() );
		doTest( settings );
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-8271")
	public void testSqlDropScriptSourceUrl() throws Exception {
		Map settings = buildSettings();
		settings.put( AvailableSettings.HBM2DDL_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE, getResourceUrlString( getDropSqlScript() ) );
		doTest( settings );
	}

	protected String getLoadSqlScript() {
		return LOAD_SQL;
	}

	protected String getCreateSqlScript() {
		return CREATE_SQL;
	}

	protected String getDropSqlScript() {
		return DROP_SQL;
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
		try {
			EntityManager em = emf.createEntityManager();
			try {
				Assert.assertNotNull( em.find( Item.class, encodedName() ) );
				Assert.assertNotNull( em.find( Item.class, "multi-file-test" ) );
			}
			finally {
				em.close();
			}
		}
		finally {
			emf.close();
			emfb.cancel();
		}
	}

	/* Disable hibernate schema export */
	@Override
	protected boolean createSchema() {
		return false;
	}

	protected String encodedName() {
		return "sch" + (char) 233 +"magen-test";
	}
}