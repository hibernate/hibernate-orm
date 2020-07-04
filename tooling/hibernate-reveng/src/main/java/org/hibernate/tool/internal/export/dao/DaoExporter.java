package org.hibernate.tool.internal.export.dao;

import java.util.Map;

import org.hibernate.tool.internal.export.java.POJOClass;
import org.hibernate.tool.internal.export.java.JavaExporter;

public class DaoExporter extends JavaExporter {

    private static final String DAO_DAOHOME_FTL = "dao/daohome.ftl";

    private String sessionFactoryName = "SessionFactory";

    public DaoExporter() {
    		super();
    }
    
    protected void init() {
    	super.init();
    	getProperties().put(TEMPLATE_NAME, DAO_DAOHOME_FTL);
    	getProperties().put(FILE_PATTERN, "{package-name}/{class-name}Home.java");
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
		getTemplateHelper().putInContext("daoHelper", new DaoHelper());
	}
	
	public String getName() {
		return "hbm2dao";
	}

	
}
