/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.enhance.spi.Enhancer;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE;
import static org.hibernate.tool.schema.internal.SchemaCreatorImpl.DEFAULT_IMPORT_FILE;

public class SkipDefaultImportFileExecutionTest {
	private File defaultImportFile;
	private StandardServiceRegistry serviceRegistry;
	private static final String COMMAND = "INSERT INTO TEST_ENTITY (id, name) values (1,'name')";


	@BeforeEach
	public void setUp() throws Exception {
		defaultImportFile = createDefaultImportFile( "import.sql" );
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder(
				new BootstrapServiceRegistryBuilder().applyClassLoader(
						toClassLoader( defaultImportFile.getParentFile() ) ).build()
		).build();
	}

	@AfterEach
	public void tearDown() {
		serviceRegistry.close();
		if ( defaultImportFile.exists() ) {
			try {
				Files.delete( defaultImportFile.toPath() );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}

	@Test
	public void testImportScriptIsNotExecutedWhitSkipDefaultImportFileSetting() {
		assertThat( serviceRegistry.getService( ClassLoaderService.class )
				.locateResource( DEFAULT_IMPORT_FILE ) ).isNotNull();

		TargetDescriptorImpl targetDescriptor = TargetDescriptorImpl.INSTANCE;

		createSchema( targetDescriptor );

		TestScriptTargetOutput targetOutput = (TestScriptTargetOutput) targetDescriptor.getScriptTargetOutput();

		assertNoInsertCommandsFromDeaultImportFileHasBeenExecuted( targetOutput );
	}

	private static void assertNoInsertCommandsFromDeaultImportFileHasBeenExecuted(TestScriptTargetOutput targetOutput) {
		assertThat( targetOutput.getInsertCommands().size() ).isEqualTo( 0 )
				.as( "Despite setting hibernate.hbm2ddl.skip_default_import_file to true the default import.sql file insert command hs been executed" );
	}

	private void createSchema(TargetDescriptorImpl targetDescriptor) {

		final Metadata mappings = buildMappings( serviceRegistry );

		final SchemaCreatorImpl schemaCreator = new SchemaCreatorImpl( serviceRegistry );

		final Map<String, Object> options = new HashMap<>();
		options.put( HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, "true" );
		schemaCreator.doCreation(
				mappings,
				new ExecutionOptionsTestImpl( options ),
				ContributableMatcher.ALL,
				SourceDescriptorImpl.INSTANCE,
				targetDescriptor
		);
	}

	private static File createDefaultImportFile(@SuppressWarnings("SameParameterValue") String fileName)
			throws Exception {
		final Path tmp = Files.createTempDirectory( "default_import" );
		final File file = new File( tmp.toString() + File.separator + fileName );

		try (final FileWriter myWriter = new FileWriter( file )) {
			myWriter.write( COMMAND );
		}

		return file;
	}

	private static ClassLoader toClassLoader(File classesDir) {
		final URI classesDirUri = classesDir.toURI();
		try {
			final URL url = classesDirUri.toURL();
			return new URLClassLoader( new URL[] {url}, Enhancer.class.getClassLoader() );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( "Unable to resolve classpath entry to URL : " + classesDir.getAbsolutePath(),
					e );
		}
	}

	private Metadata buildMappings(StandardServiceRegistry registry) {
		return new MetadataSources( registry )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		private long id;

		private String name;
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
			return EnumSet.of( TargetType.SCRIPT );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return TestScriptTargetOutput.INSTANCE;
		}
	}

	private static class TestScriptTargetOutput implements ScriptTargetOutput {

		public static final TestScriptTargetOutput INSTANCE = new TestScriptTargetOutput();

		List<String> insertCommands = new ArrayList<>();

		@Override
		public void prepare() {

		}

		@Override
		public void accept(String command) {
			if ( command.toLowerCase().startsWith( "insert" ) ) {
				insertCommands.add( command );
			}
		}

		@Override
		public void release() {

		}

		public List<String> getInsertCommands() {
			return insertCommands;
		}
	}
}
