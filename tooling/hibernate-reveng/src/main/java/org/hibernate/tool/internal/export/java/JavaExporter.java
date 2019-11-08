/*
 * Created on 2004-12-01
 *
 */
package org.hibernate.tool.internal.export.java;

import org.hibernate.tool.internal.export.common.GenericExporter;

/**
 * @author max
 */
public class JavaExporter extends GenericExporter {

	private static final String POJO_JAVACLASS_FTL = "pojo/Pojo.ftl";

	protected void init() {
    	getProperties().put(TEMPLATE_NAME, POJO_JAVACLASS_FTL);
    	getProperties().put(FILE_PATTERN, "{package-name}/{class-name}.java");
	}

	public JavaExporter() {
		init();		
	}
    
	public String getName() {
		return "hbm2java";
	}
	
	protected void setupContext() {
		//TODO: this safe guard should be in the root templates instead for each variable they depend on.
		if(!getProperties().containsKey("ejb3")) {
			getProperties().put("ejb3", "false");
		}
		if(!getProperties().containsKey("jdk5")) {
			getProperties().put("jdk5", "false");
		}	
		super.setupContext();
	}
}
