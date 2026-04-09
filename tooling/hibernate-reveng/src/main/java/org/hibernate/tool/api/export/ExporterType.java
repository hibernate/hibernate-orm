/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2018-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
