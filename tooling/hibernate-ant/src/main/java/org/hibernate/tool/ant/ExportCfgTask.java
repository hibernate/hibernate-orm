package org.hibernate.tool.ant;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;

public class ExportCfgTask {
	
	HibernateToolTask parent = null;
	Properties properties = new Properties();
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public void setDestinationFolder(File destinationFolder) {
		this.properties.put(ExporterConstants.OUTPUT_FOLDER, destinationFolder);
	}
	
	public void addConfiguredProperty(Variable variable) {
		properties.put(variable.getKey(), variable.getValue());
	}
	
	public void execute() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().putAll(this.properties);
		exporter.start();
	}

}
