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
package org.hibernate.tool.reveng.internal.exporter.ddl;

import java.io.Writer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.internal.metadata.MetadataBootstrapper;
import org.hibernate.tool.schema.SourceType;
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
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

class DdlSchemaOperations {

	private final List<ClassDetails> entities;
	private final Properties properties;
	private final String delimiter;
	private final boolean format;
	private final boolean haltOnError;

	DdlSchemaOperations(
			List<ClassDetails> entities, Properties properties,
			String delimiter, boolean format, boolean haltOnError) {
		this.entities = entities;
		this.properties = properties;
		this.delimiter = delimiter;
		this.format = format;
		this.haltOnError = haltOnError;
	}

	void createDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaCreatorImpl creator =
					new SchemaCreatorImpl(ctx.serviceRegistry());
			writeCommands(output,
					creator.generateCreationCommands(ctx.metadata(), format));
		}
	}

	void dropDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper =
					new SchemaDropperImpl(ctx.serviceRegistry());
			GenerationTarget target = scriptTarget(output);
			target.prepare();
			dropper.doDrop(ctx.metadata(), false, target);
			target.release();
		}
	}

	void bothDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper =
					new SchemaDropperImpl(ctx.serviceRegistry());
			GenerationTarget target = scriptTarget(output);
			target.prepare();
			dropper.doDrop(ctx.metadata(), false, target);
			target.release();
			SchemaCreatorImpl creator =
					new SchemaCreatorImpl(ctx.serviceRegistry());
			writeCommands(output,
					creator.generateCreationCommands(ctx.metadata(), format));
		}
	}

	void updateDdl(Writer output) {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaMigrator migrator = ctx.serviceRegistry()
					.requireService(SchemaManagementTool.class)
					.getSchemaMigrator(configurationValues());
			migrator.doMigration(
					ctx.metadata(), executionOptions(),
					ContributableMatcher.ALL,
					scriptTargetDescriptor(output));
		}
	}

	void executeCreate() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			GenerationTarget dbTarget = buildDatabaseTarget(ctx);
			SchemaCreatorImpl creator =
					new SchemaCreatorImpl(ctx.serviceRegistry());
			creator.doCreation(
					ctx.metadata(),
					ctx.serviceRegistry()
							.requireService(JdbcEnvironment.class)
							.getDialect(),
					executionOptions(), ContributableMatcher.ALL,
					metadataSourceDescriptor(), dbTarget);
		}
	}

	void executeDrop() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			GenerationTarget dbTarget = buildDatabaseTarget(ctx);
			SchemaDropperImpl dropper =
					new SchemaDropperImpl(ctx.serviceRegistry());
			dropper.doDrop(
					ctx.metadata(), executionOptions(),
					ContributableMatcher.ALL,
					ctx.serviceRegistry()
							.requireService(JdbcEnvironment.class)
							.getDialect(),
					metadataSourceDescriptor(), dbTarget);
		}
	}

	void executeBoth() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			ExecutionOptions options = executionOptions();
			SourceDescriptor source = metadataSourceDescriptor();
			SchemaDropperImpl dropper =
					new SchemaDropperImpl(ctx.serviceRegistry());
			dropper.doDrop(
					ctx.metadata(), options, ContributableMatcher.ALL,
					ctx.serviceRegistry()
							.requireService(JdbcEnvironment.class)
							.getDialect(),
					source, buildDatabaseTarget(ctx));
			SchemaCreatorImpl creator =
					new SchemaCreatorImpl(ctx.serviceRegistry());
			creator.doCreation(
					ctx.metadata(),
					ctx.serviceRegistry()
							.requireService(JdbcEnvironment.class)
							.getDialect(),
					options, ContributableMatcher.ALL,
					source, buildDatabaseTarget(ctx));
		}
	}

	void executeUpdate() {
		try (MetadataBootstrapper.MetadataContext ctx = buildMetadata()) {
			SchemaMigrator migrator = ctx.serviceRegistry()
					.requireService(SchemaManagementTool.class)
					.getSchemaMigrator(configurationValues());
			migrator.doMigration(
					ctx.metadata(), executionOptions(),
					ContributableMatcher.ALL,
					databaseTargetDescriptor());
		}
	}

	private MetadataBootstrapper.MetadataContext buildMetadata() {
		return MetadataBootstrapper.bootstrap(entities, properties);
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
		ScriptTargetOutput target = new ScriptTargetOutputToWriter(output);
		return new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of(TargetType.SCRIPT);
			}
			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return target;
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

	private GenerationTarget buildDatabaseTarget(
			MetadataBootstrapper.MetadataContext ctx) {
		HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool)
				ctx.serviceRegistry()
						.requireService(SchemaManagementTool.class);
		JdbcContext jdbcContext =
				tool.resolveJdbcContext(configurationValues());
		return new GenerationTargetToDatabase(
				tool.getDdlTransactionIsolator(jdbcContext), true, true);
	}

	private GenerationTarget scriptTarget(Writer output) {
		return new GenerationTargetToScript(
				new ScriptTargetOutputToWriter(output), delimiter);
	}

	private void writeCommands(Writer output, List<String> commands) {
		GenerationTarget target = scriptTarget(output);
		target.prepare();
		for (String command : commands) {
			target.accept(command);
		}
		target.release();
	}
}
