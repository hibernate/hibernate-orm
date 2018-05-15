/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class SchemaMigrationTargetScriptCreationTest extends SessionFactoryBasedFunctionalTest {
	private File output;

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClass( TestEntity.class );
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		super.applySettings( builer );
		try {
			output = File.createTempFile( "update_script", ".sql" );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
		output.deleteOnExit();
		builer.applySetting( Environment.HBM2DDL_DATABASE_ACTION, "update" )
				.applySetting( Environment.HBM2DDL_SCRIPTS_ACTION, "update" )
				.applySetting( Environment.HBM2DDL_SCRIPTS_CREATE_TARGET, output.getAbsolutePath() );
	}

	@AfterEach
	public void tearDown() {
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
					.addAnnotatedClass( TestEntity.class )
					.buildMetadata();
			metadata.validate();

			DatabaseModel databaseModel = Helper.buildDatabaseModel( metadata );

			new SchemaExport( databaseModel, serviceRegistry ).drop( EnumSet.of(
					TargetType.DATABASE,
					TargetType.STDOUT
			) );
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10684")
	public void testTargetScriptIsCreated() throws Exception {
		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true )
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
}
