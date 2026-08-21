/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

@Deprecated(forRemoval = true)
public class Hbm2HbmXmlExporterTask extends ExporterTask {

	public Hbm2HbmXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	protected Exporter createExporter() {
		parent.log( "The hbm2hbmxml exporter task is deprecated and will be removed in a future version. "
				+ "Use the hbm2java exporter task to generate annotated Java entities instead.",
				org.apache.tools.ant.Project.MSG_WARN );
		return ExporterFactory.createExporter(ExporterType.HBM);
	}

	public String getName() {
		return "hbm2hbmxml (Generates a set of hbm.xml files)";
	}
}
