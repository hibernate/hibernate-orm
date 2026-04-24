/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.ddl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.util.MetadataHelper;

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
		}
		else {
			File outputFile = outputFileName != null ? new File(destDir, outputFileName) : null;
			if (drop && create) {
				if (outputFile != null) configured.exportBothDdl(outputFile);
				if (exportToDatabase) configured.executeBothDdl();
				if (exportToConsole) configured.exportBothDdlToConsole();
			}
		else if (drop) {
				if (outputFile != null) configured.exportDropDdl(outputFile);
				if (exportToDatabase) configured.executeDropDdl();
				if (exportToConsole) configured.exportDropDdlToConsole();
			}
		else if (create) {
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

	public void exportCreateDdl(Writer output) {
		schemaOperations().createDdl(output);
	}

	public void exportDropDdl(Writer output) {
		schemaOperations().dropDdl(output);
	}

	public void exportBothDdl(Writer output) {
		schemaOperations().bothDdl(output);
	}

	public void exportUpdateDdl(Writer output) {
		schemaOperations().updateDdl(output);
	}

	// ---- Database execution methods ----

	public void executeCreateDdl() {
		schemaOperations().executeCreate();
	}

	public void executeDropDdl() {
		schemaOperations().executeDrop();
	}

	public void executeBothDdl() {
		schemaOperations().executeBoth();
	}

	public void executeUpdateDdl() {
		schemaOperations().executeUpdate();
	}

	// ---- Internal helpers ----

	private DdlSchemaOperations schemaOperations() {
		return new DdlSchemaOperations(
				entities, properties, delimiter, format, haltOnError);
	}

	private void writeToFile(
			File file, java.util.function.Consumer<Writer> exporter) {
		try (Writer writer = new FileWriter(file)) {
			exporter.accept(writer);
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to write DDL to " + file, e);
		}
	}

}
