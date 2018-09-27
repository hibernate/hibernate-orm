package org.hibernate.tool.api.export;

public enum ExporterType {
	
	POJO ("org.hibernate.tool.internal.export.pojo.POJOExporter");
	
	private String className;
	
	ExporterType(String className) {
		this.className = className;
	}
	
	public String className() {
		return className;
	}

}
