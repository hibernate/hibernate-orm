/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.ServiceRegistryScopeAware;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.SqlScriptException;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_HALT_ON_ERROR;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-13673")
@JiraKey("HHH-6286")
@RequiresDialect(value = H2Dialect.class,
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
@ServiceRegistry(settings = {
		@Setting(name=HBM2DDL_AUTO, value="none"),
		@Setting(name=HBM2DDL_IMPORT_FILES, value="/org/hibernate/orm/test/tool/schema/scripts/statements-without-terminal-chars.sql"),
		@Setting(name=HBM2DDL_HALT_ON_ERROR, value="true"),
		@Setting(name=HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR, value="org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor"),
})
@DomainModel
public class StatementsWithoutTerminalCharsImportFileTest implements ExecutionOptions, ServiceRegistryScopeAware {
	private static final String EXPECTED_ERROR_MESSAGE = "Import script SQL statements must terminate with a ';' char";
	private ServiceRegistryScope registryScope;

	@Test
	public void testImportFile(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final SchemaCreator schemaCreator = new SchemaCreatorImpl( registryScope.getRegistry() );

		try {
			schemaCreator.doCreation(
					modelScope.getDomainModel(),
					this,
					ContributableMatcher.ALL,
					SourceDescriptorImpl.INSTANCE,
					TargetDescriptorImpl.INSTANCE
			);
			Assertions.fail( "SqlScriptParserException expected" );
		}
		catch (SqlScriptException e) {
			assertThat( e.getMessage(), endsWith( EXPECTED_ERROR_MESSAGE ) );
		}
	}

	@Override
	public Map<String,Object> getConfigurationValues() {
		return registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return ExceptionHandlerLoggedImpl.INSTANCE;
	}

	@Override
	public void injectServiceRegistryScope(ServiceRegistryScope registryScope) {
		this.registryScope = registryScope;
	}

	private static class SourceDescriptorImpl implements SourceDescriptor {
		/**
		 * Singleton access
		 */
		public static final SourceDescriptorImpl INSTANCE = new SourceDescriptorImpl();

		@Override
		public SourceType getSourceType() {
			return SourceType.METADATA;
		}

		@Override
		public ScriptSourceInput getScriptSourceInput() {
			return null;
		}
	}

	private static class TargetDescriptorImpl implements TargetDescriptor {
		/**
		 * Singleton access
		 */
		public static final TargetDescriptorImpl INSTANCE = new TargetDescriptorImpl();

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.DATABASE );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return null;
		}
	}


	private Metadata buildMappings(StandardServiceRegistry registry) {
		return new MetadataSources( registry )
				.buildMetadata();
	}

	protected StandardServiceRegistry buildJtaStandardServiceRegistry() {
		StandardServiceRegistry registry = TestingJtaBootstrap.prepare().build();
		assertThat(
				registry.getService( TransactionCoordinatorBuilder.class ),
				instanceOf( JtaTransactionCoordinatorBuilderImpl.class )
		);
		return registry;
	}

}
