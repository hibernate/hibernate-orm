package org.hibernate.tool.hbm2x;

public interface ExporterSettings {

	public final String PREFIX_KEY = "hibernatetool.";
	
	/** 
	 * if true exporters are allowed to generate EJB3 constructs
	 */
	public final String EJB3 = PREFIX_KEY + "ejb3";
	
	/** 
	 * if true then exporters are allowed to generate JDK 5 constructs
	 */
	public final String JDK5 = PREFIX_KEY + "jdk5";
	
	/** 
	 * the (root) output directory for an exporter
	 */
	public final String OUTPUT_DIRECTORY = PREFIX_KEY + "output_directory";
	
	/** 
	 * the (root) output directory for an exporter
	 */
	public final String TEMPLATE_PATH = PREFIX_KEY + "template_path";
	
	
	
}
