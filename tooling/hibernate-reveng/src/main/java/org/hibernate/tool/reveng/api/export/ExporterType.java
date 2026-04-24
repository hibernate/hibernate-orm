/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

public enum ExporterType {

	CFG ("org.hibernate.tool.reveng.internal.exporter.cfg.CfgXmlExporter"),
	DAO ("org.hibernate.tool.reveng.internal.exporter.dao.DaoExporter"),
	DDL ("org.hibernate.tool.reveng.internal.exporter.ddl.DdlExporter"),
	DOC ("org.hibernate.tool.reveng.internal.exporter.doc.DocExporter"),
	GENERIC ("org.hibernate.tool.reveng.internal.exporter.generic.GenericExporter"),
	HBM ("org.hibernate.tool.reveng.internal.exporter.hbm.HbmXmlExporter"),
	HBM_LINT ("org.hibernate.tool.reveng.internal.exporter.lint.HbmLintExporter"),
	JAVA ("org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter"),
	QUERY ("org.hibernate.tool.reveng.internal.exporter.query.QueryExporter");

	private String className;

	ExporterType(String className) {
		this.className = className;
	}

	public String className() {
		return className;
	}

}
