/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SchemaCreateDropUtf8WithoutHbm2DdlCharsetNameTest {

	private File createSchema;
	private File dropSchema;

	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	protected Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( new Class[] {TestEntity.class} ) );
		config.put( org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}

	@Before
	public void setUp() throws IOException {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		createSchema.deleteOnExit();
		dropSchema.deleteOnExit();

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				getConfig()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10972")
	public void testEncoding() throws Exception {

		entityManagerFactoryBuilder.generateSchema();

		final String fileContent = new String( Files.readAllBytes( createSchema.toPath() ) )
				.toLowerCase();
		assertTrue( fileContent.contains( expectedTableName() ) );
		assertTrue( fileContent.contains( expectedFieldName() ) );

		final String dropFileContent = new String( Files.readAllBytes(
				dropSchema.toPath() ) ).toLowerCase();
		assertTrue( dropFileContent.contains( expectedTableName() ) );
	}

	protected String expectedTableName() {
		return "test_" + (char) 233 + "ntity";
	}

	protected String expectedFieldName() {
		return "fi" + (char) 233 + "ld";
	}

	@Entity
	@Table(name = "test_" + (char) 233 +"ntity")
	public static class TestEntity {

		@Id
		@Column(name = "fi" + (char) 233 + "ld")
		private String field;
	}
}
