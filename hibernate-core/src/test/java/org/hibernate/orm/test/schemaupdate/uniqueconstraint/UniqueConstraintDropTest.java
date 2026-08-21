/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import org.hamcrest.MatcherAssert;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaMigratorImpl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.JdbcSettings.SHOW_SQL;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name = HBM2DDL_AUTO, value = "none"),
		@Setting(name = FORMAT_SQL, value = "false"),
		@Setting(name = SHOW_SQL, value = "true")
})
@DomainModel(xmlMappings = "org/hibernate/orm/test/schemaupdate/uniqueconstraint/TestEntity.hbm.xml")
public class UniqueConstraintDropTest {

	@Test
	@JiraKey(value = "HHH-11236")
	public void testUniqueConstraintIsDropped(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		final var jdbcServices = registryScope.getRegistry().requireService( JdbcServices.class );
		final var dialect = jdbcServices.getDialect();
		final var tool = (HibernateSchemaManagementTool) registryScope.getRegistry().requireService( SchemaManagementTool.class );

		new IndividuallySchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE ).doMigration(
				metadata,
				executionOptions( registryScope.getRegistry() ),
				ContributableMatcher.ALL,
				new TargetDescriptorImpl( scriptFile )
		);

		if ( !(dialect.getUniqueDelegate() instanceof SkipNullableUniqueDelegate) ) {
			if ( dialect.getUniqueDelegate() instanceof AlterTableUniqueIndexDelegate) {
				assertTrue( checkDropIndex( scriptFile ) );
			}
			else if ( dialect.getUniqueDelegate() instanceof AlterTableUniqueDelegate ) {
				MatcherAssert.assertThat( "The test_entity_item table unique constraint has not been dropped",
						checkDropConstraint( "test_entity_item", dialect, scriptFile ),
						is( true )
				);
			}
		}

		MatcherAssert.assertThat(
				checkDropConstraint( "test_entity_children", dialect, scriptFile ),
				is( true )
		);
	}

	private ExecutionOptions executionOptions(StandardServiceRegistry registry) {
		final Map<String,Object> configurationValues = registry.requireService( ConfigurationService.class ).getSettings();
		return new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
	}

	private boolean checkDropConstraint(
			String tableName,
			Dialect dialect,
			File scriptFile) throws IOException {
		String regex = dialect.getAlterTableString( tableName ) + ' ' + dialect.getDropUniqueKeyString();
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			regex += " if exists";
		}
		regex += " uk.*";
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}
		regex += ";";
		return isMatching( regex, scriptFile );
	}

	private boolean checkDropIndex(File scriptFile) throws IOException {
		String regex = "drop index test_entity_item.uk.*";
		return isMatching( regex, scriptFile );
	}

	private boolean isMatching(String regex, File scriptFile) throws IOException {
		final String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}

	private record TargetDescriptorImpl(File scriptFile) implements TargetDescriptor {
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return new ScriptTargetOutputToFile( scriptFile, Charset.defaultCharset().name() );
		}
	}
}
