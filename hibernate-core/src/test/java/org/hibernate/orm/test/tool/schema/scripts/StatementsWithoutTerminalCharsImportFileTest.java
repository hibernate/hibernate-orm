/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.SqlScriptException;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13673")
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class StatementsWithoutTerminalCharsImportFileTest extends BaseUnitTestCase implements ExecutionOptions {
	private StandardServiceRegistry ssr;

	private static final String EXPECTED_ERROR_MESSAGE = "Import script SQL statements must terminate with a ';' char";

	@Before
	public void setUp() {
		// NOTE : the
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.applySetting(
						Environment.HBM2DDL_IMPORT_FILES,
						"/org/hibernate/orm/test/tool/schema/scripts/statements-without-terminal-chars.sql"
				).applySetting( AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true" )
				// NOTE (cont) : so here we will use the single-line variety.  we could also not
				//		specify anything as single-line is the default
				.applySetting(
						Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
						MultiLineSqlScriptExtractor.INSTANCE
				)
				.build();
	}

	@Test
	public void testImportFile() {
		final SchemaCreator schemaCreator = new SchemaCreatorImpl( ssr );

		try {
			schemaCreator.doCreation(
					buildMappings( ssr ),
					this,
					ContributableMatcher.ALL,
					SourceDescriptorImpl.INSTANCE,
					TargetDescriptorImpl.INSTANCE
			);
			fail( "SqlScriptParserException expected" );
		}
		catch (SqlScriptException e) {
			assertThat( e.getMessage(), endsWith( EXPECTED_ERROR_MESSAGE ) );
		}
	}

	@After
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Override
	public Map<String,Object> getConfigurationValues() {
		return ssr.requireService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return ExceptionHandlerLoggedImpl.INSTANCE;
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
