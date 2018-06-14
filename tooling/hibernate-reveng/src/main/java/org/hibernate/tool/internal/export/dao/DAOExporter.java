package org.hibernate.tool.internal.export.dao;

import java.util.Map;

import org.hibernate.tool.internal.export.pojo.POJOClass;
import org.hibernate.tool.internal.export.pojo.POJOExporter;

public class DAOExporter extends POJOExporter {

    private static final String DAO_DAOHOME_FTL = "dao/daohome.ftl";

    private String sessionFactoryName = "SessionFactory";

    public DAOExporter() {
    		super();
    }
    
    protected void init() {
    	super.init();
    	setTemplateName(DAO_DAOHOME_FTL);
    	setFilePattern("{package-name}/{class-name}Home.java");    	    	
    }
    
    protected void exportComponent(Map<String, Object> additionalContext, POJOClass element) {
    	// noop - we dont want components
    }

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public void setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}

	protected void setupContext() {
		getProperties().put("sessionFactoryName", getSessionFactoryName());
		super.setupContext();		
	}
	
	public String getName() {
		return "hbm2dao";
	}

	
}
