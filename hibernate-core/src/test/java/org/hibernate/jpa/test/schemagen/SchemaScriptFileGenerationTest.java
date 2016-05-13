/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaScriptFileGenerationTest {
	private File createSchema;
	private File dropSchema;
	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	@Before
	public void setUp() throws IOException {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		createSchema.deleteOnExit();
		dropSchema.deleteOnExit();

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				getConfig()
		);
	}

	@Test
	@TestForIssue(jiraKey = "10601")
	public void testGenerateSchemaDoesNotProduceTheSameStatementTwice() throws Exception {

		entityManagerFactoryBuilder.generateSchema();

		final String fileContent = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();

		assertThat( fileContent.contains( "create table test_entity" ), is( true ) );
		assertThat(
				"The statement 'create table test_entity' is generated twice",
				fileContent.replaceFirst( "create table test_entity", "" ).contains( "create table test_entity" ),
				is( false )
		);

		final String dropFileContent = new String( Files.readAllBytes( dropSchema.toPath() ) ).toLowerCase();
		assertThat( dropFileContent.contains( "drop table " ), is( true ) );
		assertThat(
				"The statement 'drop table ' is generated twice",
				dropFileContent.replaceFirst( "drop table ", "" ).contains( "drop table " ),
				is( false )
		);
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( new Class[] {TestEntity.class} ) );
		config.put( org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}

}
