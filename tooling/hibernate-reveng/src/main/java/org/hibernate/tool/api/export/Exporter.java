/*
 * Created on 2004-12-01
 */
package org.hibernate.tool.api.export;

import java.io.File;
import java.util.Properties;

/**
 * @author max and david
 * @author koen
 */
public interface Exporter {
	

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
	public void setArtifactCollector(org.hibernate.tool.api.export.ArtifactCollector collector);
	
	/**
	 * 
	 * @return artifact collector
	 */
	public ArtifactCollector getArtifactCollector();
	
	/**
	 * Called when exporter should start generating its output
	 */
	public void start();
	
}
