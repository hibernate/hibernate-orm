/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-20340")
public class NoEntitiesNoScriptsTest {

	@Test
	public void noEntitiesNoScriptsShouldSkipDDL() {
		try ( StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder().build() ) {
			TargetDescriptorImpl targetDescriptor = new TargetDescriptorImpl();

			createSchema( serviceRegistry, targetDescriptor, Map.of() );

			TestScriptTargetOutput targetOutput = (TestScriptTargetOutput) targetDescriptor.getScriptTargetOutput();

			assertThat( targetOutput.getCommands().size() ).isEqualTo( 0 );
		}
	}


	@Test
	public void noEntitiesEmptyImportFilesShouldSkipDDL() {
		try ( StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder().build() ) {
			TargetDescriptorImpl targetDescriptor = new TargetDescriptorImpl();

			Map<String, Object> options = new HashMap<>();
			options.put( AvailableSettings.HBM2DDL_IMPORT_FILES, "   " );
			createSchema( serviceRegistry, targetDescriptor, options );

			TestScriptTargetOutput targetOutput = (TestScriptTargetOutput) targetDescriptor.getScriptTargetOutput();

			assertThat( targetOutput.getInsertCommands().size() ).isEqualTo( 0 );
		}
	}

	private void createSchema(StandardServiceRegistry serviceRegistry, TargetDescriptorImpl targetDescriptor, Map<String, Object> options) {
		final Metadata mappings = buildMappings( serviceRegistry );

		assertThat( mappings.getEntityBindings().isEmpty() ).isTrue();
		assertThat( mappings.getContributors().isEmpty() ).isTrue();

		final SchemaCreatorImpl schemaCreator = new SchemaCreatorImpl( serviceRegistry );

		schemaCreator.doCreation(
				mappings,
				new ExecutionOptionsTestImpl( options ),
				ContributableMatcher.ALL,
				SourceDescriptorImpl.INSTANCE,
				targetDescriptor
		);
	}


	private Metadata buildMappings(StandardServiceRegistry registry) {
		return new MetadataSources( registry ).buildMetadata();
	}

	public class ExecutionOptionsTestImpl implements ExecutionOptions, ExceptionHandler {
		Map<String, Object> configValues;

		public ExecutionOptionsTestImpl(Map<String, Object> configValues) {
			this.configValues = configValues;
		}

		@Override
		public Map<String, Object> getConfigurationValues() {
			return configValues;
		}

		@Override
		public boolean shouldManageNamespaces() {
			return true;
		}

		@Override
		public ExceptionHandler getExceptionHandler() {
			return this;
		}

		@Override
		public void handleException(CommandAcceptanceException exception) {
			throw exception;
		}
	}

	private static class SourceDescriptorImpl implements SourceDescriptor {
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
		private final TestScriptTargetOutput targetOutput = new TestScriptTargetOutput();

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT );
		}

		@Override
		public TestScriptTargetOutput getScriptTargetOutput() {
			return targetOutput;
		}
	}

	private static class TestScriptTargetOutput implements ScriptTargetOutput {
		private final List<String> commands = new ArrayList<>();
		private final List<String> insertCommands = new ArrayList<>();

		@Override
		public void prepare() {
		}

		@Override
		public void accept(String command) {
			commands.add( command );
			if ( command.toLowerCase().startsWith( "insert" ) ) {
				insertCommands.add( command );
			}
		}

		@Override
		public void release() {
		}

		public List<String> getCommands() {
			return commands;
		}

		public List<String> getInsertCommands() {
			return insertCommands;
		}
	}
}
