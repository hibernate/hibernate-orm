/*
 * Created on 13-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.internal.export.ddl.DdlExporter;

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

	protected Exporter configureExporter(Exporter exp) {
		DdlExporter exporter = (DdlExporter) exp;
		super.configureExporter( exp );
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, exportToDatabase);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, scriptToConsole);
		exporter.getProperties().put(ExporterConstants.SCHEMA_UPDATE, schemaUpdate);
		exporter.getProperties().put(ExporterConstants.DELIMITER, delimiter);
		exporter.getProperties().put(ExporterConstants.DROP_DATABASE, drop);
		exporter.getProperties().put(ExporterConstants.CREATE_DATABASE, create);
		exporter.getProperties().put(ExporterConstants.FORMAT, format);
		if (outputFileName == null) {
			exporter.getProperties().remove(ExporterConstants.OUTPUT_FILE_NAME);
		} else {
			exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, outputFileName);
		}
		exporter.getProperties().put(ExporterConstants.HALT_ON_ERROR, haltOnError);
		return exporter;
	}

	protected Exporter createExporter() {
		DdlExporter exporter = new DdlExporter();
		exporter.getProperties().putAll(parent.getProperties());
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, parent.getProperties());
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, parent.getDestDir());
		return exporter;
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
