/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = FORMAT_SQL, value = "false"))
public class SchemaUpdateTableBackedSequenceTest {
	@Test
	public void testCreateTableOnUpdate(ServiceRegistryScope registryScope) {
		final var registry = registryScope.getRegistry();

		final var metadata = new MetadataSources( registry ).buildMetadata();
		final var database = metadata.getDatabase();
		final var tableStructure = new TableStructure(
				"orm",
				new QualifiedTableName( null, null, Identifier.toIdentifier( "test_seq" ) ),
				Identifier.toIdentifier( "nextval" ),
				20,
				30,
				Long.class
		);
		tableStructure.registerExportables( database );

		// let's make sure the InitCommand is there
		Assertions.assertEquals( 1, database.getDefaultNamespace().getTables().size() );
		Table table = database.getDefaultNamespace().getTables().iterator().next();
		SqlStringGenerationContext context = SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment(), null, null );
		Assertions.assertEquals( 1, table.getInitCommands( context ).size() );

		final TargetImpl target = new TargetImpl();

		final var migrator = registry.requireService( SchemaManagementTool.class ).getSchemaMigrator( Collections.emptyMap() );
		migrator.doMigration(
				metadata,
				new ExecutionOptions() {
					@Override
					public boolean shouldManageNamespaces() {
						return true;
					}

					@Override
					public Map<String,Object> getConfigurationValues() {
						return registry.requireService( ConfigurationService.class ).getSettings();
					}

					@Override
					public ExceptionHandler getExceptionHandler() {
						return ExceptionHandlerLoggedImpl.INSTANCE;
					}
				},
				ContributableMatcher.ALL,
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return target;
					}
				}
		);

		Assertions.assertTrue( target.found );

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	static class TargetImpl implements ScriptTargetOutput {
		boolean found = false;

		@Override
		public void prepare() {
		}

		@Override
		public void accept(String action) {
			if ( action.startsWith( "insert into test_seq" ) ) {
				found = true;
			}
		}

		@Override
		public void release() {
		}
	}
}
