package org.hibernate.tool.api.export;

public enum ExporterType {
	
	GENERIC ("org.hibernate.tool.internal.export.common.GenericExporter"),
	POJO ("org.hibernate.tool.api.export.PojoExporter");
	
	private String className;
	
	ExporterType(String className) {
		this.className = className;
	}
	
	public String className() {
		return className;
	}

}
