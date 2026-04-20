/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.exporter.ddl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.util.MetadataHelper;
import org.hibernate.tool.internal.metadata.MetadataBootstrapper;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * Generates DDL (CREATE/DROP/UPDATE) statements from a list of
 * {@link ClassDetails} entities.
 * <p>
 * Builds Hibernate {@link Metadata} from the given {@code ClassDetails}
 * by registering them in the bootstrap pipeline, then uses Hibernate's
 * schema management SPI to produce dialect-specific DDL output.
 * <p>
 * Supports three modes of operation:
 * <ul>
 *   <li><b>Script export</b> — write DDL to a {@link Writer}</li>
 *   <li><b>Database execution</b> — execute DDL against the configured database</li>
 *   <li><b>Schema update/migration</b> — generate ALTER statements by comparing
 *       metadata to the current database schema</li>
 * </ul>
 *
 * @author Koen Aers
 */
public class DdlExporter implements Exporter {

	private List<ClassDetails> entities;
	private Properties properties;
	private String delimiter = ";";
	private boolean format = false;
	private boolean haltOnError = false;
	private Properties exporterProperties = new Properties();

	public DdlExporter() {}

	@Override
	public Properties getProperties() {
		return exporterProperties;
	}

	@Override
	public void start() {
		MetadataDescriptor md = (MetadataDescriptor)
				exporterProperties.get(ExporterConstants.METADATA_DESCRIPTOR);
		File destDir = (File)
				exporterProperties.get(ExporterConstants.DESTINATION_FOLDER);
		DdlExporter configured = create(md);
		String delim = (String) exporterProperties.get(ExporterConstants.DELIMITER);
		if (delim != null) configured.delimiter(delim);
		Object fmt = exporterProperties.get(ExporterConstants.FORMAT);
		if (fmt != null) configured.format(Boolean.parseBoolean(fmt.toString()));
		Object halt = exporterProperties.get(ExporterConstants.HALT_ON_ERROR);
		if (halt != null) configured.haltOnError(Boolean.parseBoolean(halt.toString()));

		boolean create = Boolean.parseBoolean(
				String.valueOf(exporterProperties.get(ExporterConstants.CREATE_DATABASE)));
		boolean drop = Boolean.parseBoolean(
				String.valueOf(exporterProperties.get(ExporterConstants.DROP_DATABASE)));
		boolean schemaUpdate = Boolean.parseBoolean(
				String.valueOf(exporterProperties.get(ExporterConstants.SCHEMA_UPDATE)));
		boolean exportToDatabase = Boolean.parseBoolean(
				String.valueOf(exporterProperties.get(ExporterConstants.EXPORT_TO_DATABASE)));
		boolean exportToConsole = Boolean.parseBoolean(
				String.valueOf(exporterProperties.get(ExporterConstants.EXPORT_TO_CONSOLE)));
		String outputFileName = (String) exporterProperties.get(ExporterConstants.OUTPUT_FILE_NAME);

		if (schemaUpdate) {
			if (outputFileName != null) {
				configured.exportUpdateDdl(new File(destDir, outputFileName));
			}
			if (exportToDatabase) {
				configured.executeUpdateDdl();
			}
			if (exportToConsole) {
				configured.exportUpdateDdlToConsole();
			}
		} else {
			File outputFile = outputFileName != null ? new File(destDir, outputFileName) : null;
			if (drop && create) {
				if (outputFile != null) configured.exportBothDdl(outputFile);
				if (exportToDatabase) configured.executeBothDdl();
				if (exportToConsole) configured.exportBothDdlToConsole();
			} else if (drop) {
				if (outputFile != null) configured.exportDropDdl(outputFile);
				if (exportToDatabase) configured.executeDropDdl();
				if (exportToConsole) configured.exportDropDdlToConsole();
			} else if (create) {
				if (outputFile != null) configured.exportCreateDdl(outputFile);
				if (exportToDatabase) configured.executeCreateDdl();
				if (exportToConsole) configured.exportCreateDdlToConsole();
			}
		}
	}

	private DdlExporter(List<ClassDetails> entities, Properties properties) {
		this.entities = entities;
		this.properties = properties;
	}

	public static DdlExporter create(List<ClassDetails> entities, Properties properties) {
		return new DdlExporter(entities, properties);
	}

	public static DdlExporter create(MetadataDescriptor md) {
		return new DdlExporter(
				MetadataHelper.from(md).getEntityClassDetails(),
				md.getProperties());
	}

	public DdlExporter delimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public DdlExporter format(boolean format) {
		this.format = format;
		return this;
	}

	public DdlExporter haltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
		return this;
	}

	// ---- Console export methods ----

	/**
	 * Generates CREATE DDL statements and writes them to {@code System.out}.
	 */
	public void exportCreateDdlToConsole() {
		exportCreateDdl(new OutputStreamWriter(System.out));
	}

	/**
	 * Generates DROP DDL statements and writes them to {@code System.out}.
	 */
	public void exportDropDdlToConsole() {
		exportDropDdl(new OutputStreamWriter(System.out));
	}

	/**
	 * Generates both DROP and CREATE DDL statements and writes them
	 * to {@code System.out} (drop first, then create).
	 */
	public void exportBothDdlToConsole() {
		exportBothDdl(new OutputStreamWriter(System.out));
	}

	/**
	 * Generates schema migration (ALTER) DDL and writes it to {@code System.out}.
	 */
	public void exportUpdateDdlToConsole() {
		exportUpdateDdl(new OutputStreamWriter(System.out));
	}

	// ---- File export methods ----

	/**
	 * Generates CREATE DDL statements and writes them to the given file.
	 */
	public void exportCreateDdl(File outputFile) {
		writeToFile(outputFile, this::exportCreateDdl);
	}

	/**
	 * Generates DROP DDL statements and writes them to the given file.
	 */
	public void exportDropDdl(File outputFile) {
		writeToFile(outputFile, this::exportDropDdl);
	}

	/**
	 * Generates both DROP and CREATE DDL statements and writes them
	 * to the given file (drop first, then create).
	 */
	public void exportBothDdl(File outputFile) {
		writeToFile(outputFile, this::exportBothDdl);
	}

	/**
	 * Generates schema migration (ALTER) DDL and writes it to the given file.
	 */
	public void exportUpdateDdl(File outputFile) {
		writeToFile(outputFile, this::exportUpdateDdl);
	}

	// ---- Writer export methods ----

	/**
	 * Generates CREATE DDL statements and writes them to the given writer.
	 */
	public void exportCreateDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry());
			List<String> commands = creator.generateCreationCommands(ctx.metadata(), format);
			writeCommands(output, commands);
		}
	}

	/**
	 * Generates DROP DDL statements and writes them to the given writer.
	 */
	public void exportDropDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry());
			ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
			GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
			target.prepare();
			dropper.doDrop(ctx.metadata(), false, target);
			target.release();
		}
	}

	/**
	 * Generates both DROP and CREATE DDL statements and writes them
	 * to the given writer (drop first, then create).
	 */
	public void exportBothDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry());
			ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
			GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
			target.prepare();
			dropper.doDrop(ctx.metadata(), false, target);
			target.release();

			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry());
			List<String> commands = creator.generateCreationCommands(ctx.metadata(), format);
			writeCommands(output, commands);
		}
	}

	/**
	 * Generates schema migration (ALTER) DDL by comparing the metadata model
	 * to the current database schema, and writes the statements to the given writer.
	 * <p>
	 * Requires database connection properties (e.g. {@code hibernate.connection.url})
	 * because the migrator must inspect the live database schema.
	 */
	public void exportUpdateDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaMigrator migrator = ctx.serviceRegistry()
					.requireService(SchemaManagementTool.class)
					.getSchemaMigrator(configurationValues());
			migrator.doMigration(
					ctx.metadata(),
					executionOptions(),
					ContributableMatcher.ALL,
					scriptTargetDescriptor(output));
		}
	}

	// ---- Database execution methods (execute DDL against the database) ----

	/**
	 * Executes CREATE DDL statements against the configured database.
	 */
	public void executeCreateDdl() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			GenerationTarget dbTarget = buildDatabaseTarget(ctx);
			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry());
			creator.doCreation(ctx.metadata(), ctx.serviceRegistry().requireService(JdbcEnvironment.class).getDialect(), executionOptions(),
					ContributableMatcher.ALL, metadataSourceDescriptor(), dbTarget);
		}
	}

	/**
	 * Executes DROP DDL statements against the configured database.
	 */
	public void executeDropDdl() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			GenerationTarget dbTarget = buildDatabaseTarget(ctx);
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry());
			dropper.doDrop(ctx.metadata(), executionOptions(), ContributableMatcher.ALL,
					ctx.serviceRegistry().requireService(JdbcEnvironment.class).getDialect(), metadataSourceDescriptor(), dbTarget);
		}
	}

	/**
	 * Executes both DROP and CREATE DDL statements against the
	 * configured database (drop first, then create).
	 */
	public void executeBothDdl() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			ExecutionOptions options = executionOptions();
			SourceDescriptor source = metadataSourceDescriptor();

			GenerationTarget dropTarget = buildDatabaseTarget(ctx);
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry());
			dropper.doDrop(ctx.metadata(), options, ContributableMatcher.ALL,
					ctx.serviceRegistry().requireService(JdbcEnvironment.class).getDialect(), source, dropTarget);

			GenerationTarget createTarget = buildDatabaseTarget(ctx);
			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry());
			creator.doCreation(ctx.metadata(), ctx.serviceRegistry().requireService(JdbcEnvironment.class).getDialect(), options,
					ContributableMatcher.ALL, source, createTarget);
		}
	}

	/**
	 * Executes schema migration (ALTER) DDL against the configured database
	 * by comparing the metadata model to the current database schema.
	 */
	public void executeUpdateDdl() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaMigrator migrator = ctx.serviceRegistry()
					.requireService(SchemaManagementTool.class)
					.getSchemaMigrator(configurationValues());
			migrator.doMigration(
					ctx.metadata(),
					executionOptions(),
					ContributableMatcher.ALL,
					databaseTargetDescriptor());
		}
	}

	// ---- Internal helpers ----

	private void writeToFile(File file, java.util.function.Consumer<Writer> exporter) {
		try (Writer writer = new FileWriter(file)) {
			exporter.accept(writer);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to write DDL to " + file, e);
		}
	}

	private Map<String, Object> configurationValues() {
		Map<String, Object> values = new HashMap<>();
		for (String key : properties.stringPropertyNames()) {
			values.put(key, properties.getProperty(key));
		}
		return values;
	}

	private ExecutionOptions executionOptions() {
		Map<String, Object> config = configurationValues();
		ExceptionHandler handler = haltOnError
				? ExceptionHandlerHaltImpl.INSTANCE
				: ExceptionHandlerLoggedImpl.INSTANCE;
		return new ExecutionOptions() {
			@Override
			public Map<String, Object> getConfigurationValues() {
				return config;
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return handler;
			}
		};
	}

	private SourceDescriptor metadataSourceDescriptor() {
		return new SourceDescriptor() {
			@Override
			public SourceType getSourceType() {
				return SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};
	}

	private TargetDescriptor scriptTargetDescriptor(Writer output) {
		ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
		return new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of(TargetType.SCRIPT);
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return scriptTarget;
			}
		};
	}

	private TargetDescriptor databaseTargetDescriptor() {
		return new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of(TargetType.DATABASE);
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return null;
			}
		};
	}

	private GenerationTarget buildDatabaseTarget(MetadataBootstrapper.MetadataContext ctx) {
		HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool)
				ctx.serviceRegistry().requireService(SchemaManagementTool.class);
		JdbcContext jdbcContext = tool.resolveJdbcContext(configurationValues());
		return new GenerationTargetToDatabase(
				tool.getDdlTransactionIsolator(jdbcContext), true, true);
	}

	private MetadataBootstrapper.MetadataContext buildMetadata() {
		return MetadataBootstrapper.bootstrap(entities, properties);
	}

	private void writeCommands(Writer output, List<String> commands) {
		ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
		GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
		target.prepare();
		for (String command : commands) {
			target.accept(command);
		}
		target.release();
	}

}
