/*
 * Created on 2004-12-01
 */
package org.hibernate.tool.hbm2x;

import java.io.File;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.exporter.DefaultArtifactCollector;

/**
 * @author max and david
 * @author koen
 */
public interface Exporter {
	
	/** 
	 * @param metadataDescriptor An Hibernate {@link org.hibernate.tool.api.metadata.MetadataDescriptor} or subclass instance that defines the hibernate meta model to be exported.
	 */
	public void setMetadataDescriptor(MetadataDescriptor metadataDescriptor);
		

	public Metadata getMetadata();
	
	/**
	 * @param file basedirectory to be used for generated files.
	 */
	public void setOutputDirectory(File file);

	public File getOutputDirectory();
	
	/**
	 * @param templatePath array of directories used sequentially to lookup templates
	 */
	public void setTemplatePath(String[] templatePath);
	
	public String[] getTemplatePath();
		
	public Properties getProperties();
	
	/**
	 * 
	 * @param collector Instance to be consulted when adding a new file.
	 */
	public void setArtifactCollector(DefaultArtifactCollector collector);
	
	/**
	 * 
	 * @return artifact collector
	 */
	public DefaultArtifactCollector getArtifactCollector();
	
	/**
	 * Called when exporter should start generating its output
	 */
	public void start();
	
}
