/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Test;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class SchemaMigrationTargetScriptCreationTest extends BaseCoreFunctionalTestCase {
	private File output;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected void configure(Configuration configuration) {
		try {
			output = File.createTempFile( "update_script", ".sql" );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
		output.deleteOnExit();
		configuration.setProperty( Environment.JAKARTA_HBM2DDL_DATABASE_ACTION, "update" );
		configuration.setProperty( Environment.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "update" );
		configuration.setProperty( Environment.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, output.getAbsolutePath() );
	}

	@After
	public void tearDown() {
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
					.addAnnotatedClass( TestEntity.class )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey(value = "HHH-10684")
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
