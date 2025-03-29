/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Test;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Jonathan Bregler
 */
@RequiresDialect(value = HANADialect.class)
public class HANASchemaMigrationTargetScriptCreationTest extends BaseCoreFunctionalTestCase {

	private File output;
	private String varcharType;
	private String clobType;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ TestEntity.class };
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected void configure(Configuration configuration) {
		try {
			this.output = File.createTempFile( "update_script", ".sql" );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
		this.output.deleteOnExit();
		configuration.setProperty( Environment.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" );
		configuration.setProperty( Environment.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, this.output.getAbsolutePath() );
	}

	@Override
	protected void afterSessionFactoryBuilt() {
		super.afterSessionFactoryBuilt();
		final Dialect dialect = sessionFactory().getJdbcServices().getDialect();
		this.varcharType = ( (HANADialect) dialect ).isUseUnicodeStringTypes() ? "nvarchar" : "varchar";
		this.clobType = ( (HANADialect) dialect ).isUseUnicodeStringTypes() ? "nclob" : "clob";
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
	@JiraKey(value = "HHH-12302")
	public void testTargetScriptIsCreatedStringTypeDefault() throws Exception {
		this.rebuildSessionFactory();
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12302")
	public void testTargetScriptIsCreatedStringTypeNVarchar() throws Exception {
		this.rebuildSessionFactory( config -> {
			config.setProperty( "hibernate.dialect.hana.use_unicode_string_types", "true" );
		} );
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c nvarchar[^,]+, field nvarchar[^,]+, lob nclob" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12302")
	public void testTargetScriptIsCreatedStringTypeVarchar() throws Exception {
		this.rebuildSessionFactory( config -> {
			config.setProperty( "hibernate.dialect.hana.use_unicode_string_types", "false" );
		} );
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	public void testTargetScriptIsCreatedBooleanTypeDefault() throws Exception {
		this.rebuildSessionFactory();
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	public void testTargetScriptIsCreatedBooleanTypeLegacy() throws Exception {
		this.rebuildSessionFactory( config -> {
			config.setProperty( "hibernate.dialect.hana.use_legacy_boolean_type", "true" );
		} );
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b tinyint[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	public void testTargetScriptIsCreatedBooleanType() throws Exception {
		this.rebuildSessionFactory( config -> {
			config.setProperty( "hibernate.dialect.hana.use_legacy_boolean_type", "false" );
		} );
		String fileContent = new String( Files.readAllBytes( this.output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {

		@Id
		private String field;

		private char c;

		@Lob
		private String lob;

		private boolean b;
	}
}
