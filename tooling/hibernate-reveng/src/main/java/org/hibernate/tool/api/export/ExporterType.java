package org.hibernate.tool.api.export;

public enum ExporterType {
	
	CFG ("org.hibernate.tool.internal.export.cfg.CfgExporter"),
	DAO ("org.hibernate.tool.internal.export.dao.DaoExporter"),
	DDL ("org.hibernate.tool.internal.export.ddl.DdlExporter"),
	DOC ("org.hibernate.tool.internal.export.doc.DocExporter"),
	GENERIC ("org.hibernate.tool.internal.export.common.GenericExporter"),
	HBM ("org.hibernate.tool.internal.export.hbm.HbmExporter"),
	HBM_LINT ("org.hibernate.tool.internal.export.lint.HbmLintExporter"),
	JAVA ("org.hibernate.tool.internal.export.java.JavaExporter"),
	QUERY ("org.hibernate.tool.internal.export.query.QueryExporter");
	
	private String className;
	
	ExporterType(String className) {
		this.className = className;
	}
	
	public String className() {
		return className;
	}

}
