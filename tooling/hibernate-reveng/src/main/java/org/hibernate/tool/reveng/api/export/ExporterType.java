/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

public enum ExporterType {

	CFG ("org.hibernate.tool.reveng.internal.export.cfg.CfgExporter"),
	DAO ("org.hibernate.tool.reveng.internal.export.dao.DaoExporter"),
	DDL ("org.hibernate.tool.reveng.internal.export.ddl.DdlExporter"),
	DOC ("org.hibernate.tool.reveng.internal.export.doc.DocExporter"),
	GENERIC ("org.hibernate.tool.reveng.internal.export.common.GenericExporter"),
	HBM ("org.hibernate.tool.reveng.internal.export.hbm.HbmExporter"),
	HBM_LINT ("org.hibernate.tool.reveng.internal.export.lint.HbmLintExporter"),
	JAVA ("org.hibernate.tool.reveng.internal.export.java.JavaExporter"),
	QUERY ("org.hibernate.tool.reveng.internal.export.query.QueryExporter");

	private String className;

	ExporterType(String className) {
		this.className = className;
	}

	public String className() {
		return className;
	}

}
