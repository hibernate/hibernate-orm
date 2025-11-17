/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;

/**
 * @author Jonathan Bregler
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(HANADialect.class)
public class HANASchemaMigrationTargetScriptCreationTest {
	private String varcharType;
	private String clobType;

	@BeforeEach
	void setUp(DomainModelScope modelScope) {
		final HANADialect dialect = (HANADialect) modelScope.getDomainModel().getDatabase().getDialect();
		this.varcharType = dialect.isUseUnicodeStringTypes() ? "nvarchar" : "varchar";
		this.clobType = dialect.isUseUnicodeStringTypes() ? "nclob" : "clob";
	}

	@Test
	@JiraKey(value = "HHH-12302")
	@ServiceRegistry
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedStringTypeDefault(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12302")
	@ServiceRegistry(settings = {
			@Setting(name = "hibernate.dialect.hana.use_unicode_string_types", value = "true")
	})
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedStringTypeNVarchar(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c nvarchar[^,]+, field nvarchar[^,]+, lob nclob" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12302")
	@ServiceRegistry
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedStringTypeVarchar(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@ServiceRegistry
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedBooleanTypeDefault(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@ServiceRegistry(settings = {
			@Setting(name = "hibernate.dialect.hana.use_legacy_boolean_type", value = "true")
	})
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedBooleanTypeLegacy(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b tinyint[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-12132")
	@ServiceRegistry
	@DomainModel(annotatedClasses = HANASchemaMigrationTargetScriptCreationTest.TestEntity.class)
	public void testTargetScriptIsCreatedBooleanType(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "script.sql" );

		exportToScriptFile( registryScope, modelScope, scriptFile );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity \\(b boolean[^,]+, c " + this.varcharType + "[^,]+, field " + this.varcharType + "[^,]+, lob " + this.clobType );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
	}

	private void exportToScriptFile(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			File scriptFile) {
		var schemaCreator = new SchemaCreatorImpl( registryScope.getRegistry() );
		final GenerationTargetToScript target =
				new GenerationTargetToScript( new ScriptTargetOutputToFile( scriptFile, "utf8" ), ";" );
		modelScope.getDomainModel().orderColumns( true );
		try {
			target.prepare();
			final Map<String, Object> settings = registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();
			schemaCreator.createFromMetadata(
					modelScope.getDomainModel(),
					new ExecutionOptions() {
						@Override
						public Map<String, Object> getConfigurationValues() {
							return settings;
						}

						@Override
						public boolean shouldManageNamespaces() {
							return false;
						}

						@Override
						public ExceptionHandler getExceptionHandler() {
							return ExceptionHandlerHaltImpl.INSTANCE;
						}
					},
					registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect(),
					source -> source,
					target
			);
		}
		finally {
			target.release();
		}
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
