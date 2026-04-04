/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;

import java.util.EnumSet;

/**
 * @author max
 *
 */
public class Hbm2DDLExporterTask extends ExporterTask {

	boolean exportToDatabase = true;
	boolean scriptToConsole = true;
	boolean schemaUpdate = false;
	String delimiter = ";";
	boolean drop = false;
	boolean create = true;
	boolean format = false;

	String outputFileName = null;
	private boolean haltOnError = false;

	public Hbm2DDLExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public String getName() {
		return "hbm2ddl (Generates database schema)";
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		Metadata metadata = md.createMetadata();

		if (schemaUpdate) {
			performSchemaUpdate(metadata);
		} else {
			performSchemaExport(metadata);
		}
	}

	private void performSchemaExport(Metadata metadata) {
		var serviceRegistry = metadata.getDatabase().getServiceRegistry();
		SchemaCreatorImpl creator = new SchemaCreatorImpl(serviceRegistry);
		SchemaDropperImpl dropper = new SchemaDropperImpl(serviceRegistry);

		List<GenerationTarget> targets = buildTargets(metadata);
		try {
			for (GenerationTarget target : targets) {
				target.prepare();
			}
			GenerationTarget[] targetArray =
					targets.toArray(new GenerationTarget[0]);
			if (drop) {
				dropper.doDrop(metadata, false, targetArray);
			}
			if (create) {
				creator.doCreation(metadata, false, targetArray);
			}
		} finally {
			for (GenerationTarget target : targets) {
				target.release();
			}
		}
	}

	private void performSchemaUpdate(Metadata metadata) {
		var serviceRegistry = metadata.getDatabase().getServiceRegistry();
		Map<String, Object> configValues = configurationValues();
		HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool)
				serviceRegistry.requireService(SchemaManagementTool.class);
		SchemaMigrator migrator = tool.getSchemaMigrator(configValues);
		ExecutionOptions options = executionOptions();
		EnumSet<TargetType> targetTypes = EnumSet.noneOf(TargetType.class);
		if (exportToDatabase) {
			targetTypes.add(TargetType.DATABASE);
		}
		if (scriptToConsole) {
			targetTypes.add(TargetType.STDOUT);
		}
		ScriptTargetOutput scriptTarget = null;
		if (outputFileName != null) {
			targetTypes.add(TargetType.SCRIPT);
			File outputFile = new File(getDestdir(), outputFileName);
			outputFile.getParentFile().mkdirs();
			try {
				scriptTarget = new ScriptTargetOutputToWriter(
						new FileWriter(outputFile));
			} catch (IOException e) {
				throw new RuntimeException("Cannot write to " + outputFile, e);
			}
		}
		final ScriptTargetOutput fScriptTarget = scriptTarget;
		TargetDescriptor targetDescriptor = new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return targetTypes;
			}
			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return fScriptTarget;
			}
		};
		migrator.doMigration(metadata, options,
				ContributableMatcher.ALL, targetDescriptor);
	}

	private List<GenerationTarget> buildTargets(Metadata metadata) {
		List<GenerationTarget> targets = new ArrayList<>();
		if (exportToDatabase) {
			var serviceRegistry = metadata.getDatabase().getServiceRegistry();
			HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool)
					serviceRegistry.requireService(SchemaManagementTool.class);
			JdbcContext jdbcContext = tool.resolveJdbcContext(configurationValues());
			targets.add(new GenerationTargetToDatabase(
					tool.getDdlTransactionIsolator(jdbcContext)));
		}
		if (outputFileName != null) {
			File outputFile = new File(getDestdir(), outputFileName);
			outputFile.getParentFile().mkdirs();
			try {
				targets.add(new GenerationTargetToScript(
						new ScriptTargetOutputToWriter(new FileWriter(outputFile)),
						delimiter));
			} catch (IOException e) {
				throw new RuntimeException("Cannot write to " + outputFile, e);
			}
		}
		if (scriptToConsole) {
			targets.add(new GenerationTargetToStdout(delimiter));
		}
		return targets;
	}

	private ExecutionOptions executionOptions() {
		ExceptionHandler handler = haltOnError
				? ExceptionHandlerHaltImpl.INSTANCE
				: ExceptionHandlerLoggedImpl.INSTANCE;
		return new ExecutionOptions() {
			@Override
			public Map<String, Object> getConfigurationValues() {
				return configurationValues();
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

	private Map<String, Object> configurationValues() {
		Map<String, Object> values = new HashMap<>();
		for (Map.Entry<Object, Object> e : parent.getProperties().entrySet()) {
			values.put(String.valueOf(e.getKey()), e.getValue());
		}
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			values.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (delimiter != null) {
			values.put("hibernate.hbm2ddl.delimiter", delimiter);
		}
		if (format) {
			values.put("hibernate.format_sql", "true");
		}
		return values;
	}

	protected Exporter createExporter() {
		return null;
	}

	public void setExport(boolean export) {
		exportToDatabase = export;
	}

	/**
	 * Run SchemaUpdate instead of SchemaExport
	 */
	public void setUpdate(boolean update) {
		this.schemaUpdate = update;
	}

	/**
	 * Output sql to console ? (default true)
	 */
	public void setConsole(boolean console) {
		this.scriptToConsole = console;
	}

	/**
	 * Format the generated sql
	 */
	public void setFormat(boolean format) {
		this.format = format;
	}

	/**
	 * File out put name (default: empty)
	 */
	public void setOutputFileName(String fileName) {
		outputFileName = fileName;
	}

	public void setDrop(boolean drop) {
		this.drop = drop;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError  = haltOnError;
	}
}
