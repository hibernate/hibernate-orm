/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import java.net.URL;
import java.util.Map;
import java.util.function.Function;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class JpaSchemaGeneratorTest extends EntityManagerFactoryBasedFunctionalTest {

	private final String LOAD_SQL = getScriptFolderPath() + "load-script-source.sql , " + getScriptFolderPath() + "load-script-source2.sql";
	private final String CREATE_SQL = getScriptFolderPath() + "create-script-source.sql , " + getScriptFolderPath() + "create-script-source2.sql";
	private final String DROP_SQL = getScriptFolderPath() + "drop-script-source.sql , " + getScriptFolderPath() + "drop-script-source2.sql";

	private static int schemagenNumber = 0;

	public String getScriptFolderPath() {
		return "org/hibernate/orm/test/jpa/schemagen/";
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlLoadScriptSourceClasspath() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, getLoadSqlScript() );
		doTest( settings );
	}


	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlLoadScriptSourceUrl() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, getResourceUrlString( getLoadSqlScript() ) );
		doTest( settings );
	}

	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlCreateScriptSourceClasspath() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE, getCreateSqlScript() );
		doTest( settings );
	}

	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlCreateScriptSourceUrl() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE, getResourceUrlString( getCreateSqlScript() ) );
		doTest( settings );
	}


	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlDropScriptSourceClasspath() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE, getDropSqlScript() );
		doTest( settings );
	}

	@Test
	@JiraKey(value = "HHH-8271")
	public void testSqlDropScriptSourceUrl() {
		Map<Object, Object> settings = buildSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DROP_SOURCE, "metadata-then-script" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "drop" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE, getResourceUrlString( getDropSqlScript() ) );
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

	protected String encodedName() {
		return "sch" + (char) 233 + "magen-test";
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

	private void doTest(Map<Object, Object> settings) {
		// We want a fresh db after emf close
		// Unfortunately we have to use this dirty hack because the db seems not to be closed otherwise
		settings.put( "hibernate.connection.url", "jdbc:h2:mem:db-schemagen" + schemagenNumber++
				+ ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE" );
		EntityManagerFactoryBuilder emfb = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				settings
		);
		EntityManagerFactory emf = emfb.build();
		try {
			EntityManager em = emf.createEntityManager();
			try {
				Assertions.assertNotNull( em.find( Item.class, encodedName() ) );
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
}
