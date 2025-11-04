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
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;

/**
 * @author max
 * 
 * Is not actually a ant task, but simply just a task part of a HibernateToolTask
 *
 */
public abstract class ExporterTask {

	// refactor out so not dependent on Ant ?
	protected HibernateToolTask parent;
	Properties properties;
	private Path templatePath;
	
	public ExporterTask(HibernateToolTask parent) {
		this.parent = parent;
		this.properties = new Properties();
	}

	
	/*final*/ public void execute() {
	
		Exporter exporter = configureExporter(createExporter() );
		exporter.start();
		
	}
	
	protected abstract Exporter createExporter();

	public File getDestdir() {
		File destdir = (File)this.properties.get(ExporterConstants.DESTINATION_FOLDER);
		if(destdir==null) {
			return parent.getDestDir();
		} 
		else {
			return destdir;
		}
	}
	public void setDestdir(File destdir) {
		this.properties.put(ExporterConstants.DESTINATION_FOLDER, destdir);
	}
	
	public void setTemplatePath(Path path) {
		templatePath = path;
	}
	
	public void validateParameters() {
		if(getDestdir()==null) {
			throw new BuildException("destdir must be set, either locally or on <hibernatetool>");
		}		
	}
	
	public void addConfiguredPropertySet(PropertySet ps) {
		properties.putAll(ps.getProperties());
	}
	
	public void addConfiguredProperty(Environment.Variable property) {
		properties.put(property.getKey(), property.getValue());
	}
	
	protected Path getTemplatePath() {
		if(templatePath==null) {
			return parent.getTemplatePath();
		} 
		else {
			return templatePath;
		}		
	}
	
	
	abstract String getName();
	
	protected Exporter configureExporter(Exporter exporter) {
		Properties prop = new Properties();
		prop.putAll(parent.getProperties());
		prop.putAll(properties);
		exporter.getProperties().putAll(prop);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, parent.getMetadataDescriptor());
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, getDestdir());
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, getTemplatePath().list());
		return exporter;
	}
}
