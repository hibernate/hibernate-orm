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

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;

/**
 * @author max
 *
 */
public class GenericExporterTask extends ExporterTask {

	public GenericExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	String templateName;
	String exporterClass;
	String filePattern;
	String forEach;
	
	/**
	 * The FilePattern defines the pattern used to generate files.
	 * @param filePattern
	 */
	public void setFilePattern(String filePattern) {
		this.filePattern = filePattern;
	}
	
	public void setForEach(String forEach) {
		this.forEach = forEach;
	}
	
	public void setTemplate(String templateName) {
		this.templateName = templateName;
	}
	
	public void setExporterClass(String exporterClass) {
		this.exporterClass = exporterClass;
	}
	
	protected Exporter createExporter() {
		if (exporterClass == null) {
			return ExporterFactory.createExporter(ExporterType.GENERIC);
		} else {
			return ExporterFactory.createExporter(exporterClass);
		}		
	}
	
	protected Exporter configureExporter(Exporter exp) {
		super.configureExporter(exp);
		if (templateName != null) {
			exp.getProperties().put(ExporterConstants.TEMPLATE_NAME, templateName);
		}
		if (filePattern != null) {
			exp.getProperties().put(ExporterConstants.FILE_PATTERN, filePattern);
		}
		if (forEach != null) {
			exp.getProperties().put(ExporterConstants.FOR_EACH, forEach);
		}
		return exp;
	}

	public String getName() {
		StringBuffer buf = new StringBuffer("generic exporter");
		if(exporterClass!=null) {
			buf.append( "class: " + exporterClass);
		}
		if(templateName!=null) {
			buf.append( "template: " + templateName);
		}
		return buf.toString();
	}
}
