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
	 * Called when exporter should start generating its output
	 */
	public void start();
	
}
