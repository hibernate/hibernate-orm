/*
 * Created on 14-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.internal.export.common.GenericExporter;
import org.hibernate.tool.internal.util.ReflectHelper;

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
			return new GenericExporter();
		} else {
			try {
				return (Exporter) ReflectHelper.classForName(exporterClass).newInstance();
			} catch (ClassNotFoundException e) {
				throw new BuildException("Could not find custom exporter class: " + exporterClass, e);
			} catch (InstantiationException e) {
				throw new BuildException("Could not create custom exporter class: " + exporterClass, e);
			} catch (IllegalAccessException e) {
				throw new BuildException("Could not access custom exporter class: " + exporterClass, e);
			}
		}		
	}
	
	protected Exporter configureExporter(Exporter exp) {
		super.configureExporter(exp);
		
		if(exp instanceof GenericExporter) {
			GenericExporter exporter = (GenericExporter) exp;
			if(filePattern!=null) exporter.setFilePattern(filePattern);
			if(templateName!=null) exporter.setTemplateName(templateName);
			if(forEach!=null) exporter.setForEach(forEach);
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
