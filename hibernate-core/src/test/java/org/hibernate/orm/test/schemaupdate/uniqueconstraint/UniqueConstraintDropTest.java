/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
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

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintDropTest {
	private File output;
	private MetadataImplementor metadata;
	private StandardServiceRegistry ssr;
	private HibernateSchemaManagementTool tool;
	private ExecutionOptions options;

	@Before
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.applySetting( Environment.FORMAT_SQL, "false" )
				.applySetting( Environment.SHOW_SQL, "true" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addResource( "org/hibernate/orm/test/schemaupdate/uniqueconstraint/TestEntity.hbm.xml" )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		final Map<String,Object> configurationValues = ssr.requireService( ConfigurationService.class ).getSettings();
		options = new ExecutionOptions() {
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

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@JiraKey(value = "HHH-11236")
	public void testUniqueConstraintIsDropped() throws Exception {

		new IndividuallySchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE )
				.doMigration(
						metadata,
						options,
						ContributableMatcher.ALL,
						new TargetDescriptorImpl()
				);

		if ( !(getDialect().getUniqueDelegate() instanceof SkipNullableUniqueDelegate) ) {
			if ( getDialect().getUniqueDelegate() instanceof AlterTableUniqueIndexDelegate) {
				checkDropIndex( "test_entity_item", "item" );
			}
			else if ( getDialect().getUniqueDelegate() instanceof AlterTableUniqueDelegate ) {
				assertThat(
						"The test_entity_item table unique constraint has not been dropped",
						checkDropConstraint( "test_entity_item", "item" ),
						is( true )
				);
			}
		}

		assertThat(
				checkDropConstraint( "test_entity_children", "child" ),
				is( true )
		);
	}

	protected Dialect getDialect() {
		return ssr.getService( JdbcEnvironment.class ).getDialect();
	}

	private boolean checkDropConstraint(String tableName, String columnName) throws IOException {
		String regex = getDialect().getAlterTableString( tableName ) + ' ' + getDialect().getDropUniqueKeyString();
		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
			regex += " if exists";
		}
		regex += " uk.*";
		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}
		regex += ";";
		return isMatching( regex );
	}

//	private boolean checkAlterTableDropIndex(String tableName, String columnName) throws IOException {
//		String regex = "alter table ";
//
//		if ( getDialect().supportsIfExistsAfterAlterTable() ) {
//			regex += "if exists ";
//		}
//		regex += tableName;
//		if ( getDialect().supportsIfExistsAfterTableName() ) {
//			regex += " if exists";
//		}
//		regex += " drop index";
//
//		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
//			regex += " if exists";
//		}
//		regex += " uk.*";
//		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
//			regex += " if exists";
//		}
//
//		return isMatching( regex );
//	}

	private boolean checkDropIndex(String tableName, String columnName) throws IOException {
		String regex = "drop index " + tableName + ".uk.*";
		return isMatching( regex );
	}

	private boolean isMatching(String regex) throws IOException {
		final String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase();
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

	private class TargetDescriptorImpl implements TargetDescriptor {
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return new ScriptTargetOutputToFile( output, Charset.defaultCharset().name() );
		}
	}
}
