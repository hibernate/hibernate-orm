/*
 * Created on 2004-12-01
 *
 */
package org.hibernate.tool.internal.export.pojo;

import org.hibernate.tool.internal.export.common.GenericExporter;

/**
 * @author max
 */
public class POJOExporter extends GenericExporter {

	private static final String POJO_JAVACLASS_FTL = "pojo/Pojo.ftl";

	protected void init() {
		setTemplateName(POJO_JAVACLASS_FTL);
    	setFilePattern("{package-name}/{class-name}.java");    	
	}

	public POJOExporter() {
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
