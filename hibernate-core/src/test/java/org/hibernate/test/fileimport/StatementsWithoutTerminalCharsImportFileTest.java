/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.fileimport;

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
import org.hibernate.hql.internal.antlr.SqlStatementParser;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.tool.hbm2ddl.ImportScriptException;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.schemaupdate.CommentGenerationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13673")
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class StatementsWithoutTerminalCharsImportFileTest extends BaseUnitTestCase implements ExecutionOptions {


	private StandardServiceRegistry ssr;
	private static final String EXPECTED_ERROR_MESSAGE = "Import script Sql statements must terminate with a ';' char";

	@Before
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.applySetting( Environment.DIALECT, CommentGenerationTest.SupportCommentDialect.class.getName() )
				.applySetting(
						Environment.HBM2DDL_IMPORT_FILES,
						"/org/hibernate/test/fileimport/statements-without-terminal-chars.sql"
				).applySetting( AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true" )
				.applySetting(
						Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
						MultipleLinesSqlCommandExtractor.class.getName()
				)
				.build();
	}

	@Test
	public void testImportFile() {
		try {
			final SchemaCreator schemaCreator = new SchemaCreatorImpl( ssr );

			schemaCreator.doCreation(
					buildMappings( ssr ),
					this,
					SourceDescriptorImpl.INSTANCE,
					TargetDescriptorImpl.INSTANCE
			);

			fail( "ImportScriptException expected" );
		}
		catch (ImportScriptException e) {
			final Throwable cause = e.getCause();

			assertThat( cause, instanceOf( SqlStatementParser.StatementParserException.class ) );

			assertThat( cause.getMessage(), is( EXPECTED_ERROR_MESSAGE ) );
		}
	}

	@After
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Override
	public Map getConfigurationValues() {
		return ssr.getService( ConfigurationService.class ).getSettings();
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
