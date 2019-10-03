package org.hibernate.tool.api.export;

public enum ExporterType {
	
	CFG ("org.hibernate.tool.internal.export.cfg.CfgExporter"),
	DAO ("org.hibernate.tool.internal.export.dao.DAOExporter"),
	GENERIC ("org.hibernate.tool.internal.export.common.GenericExporter"),
	POJO ("org.hibernate.tool.internal.export.pojo.POJOExporter");
	
	private String className;
	
	ExporterType(String className) {
		this.className = className;
	}
	
	public String className() {
		return className;
	}

}
