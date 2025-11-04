/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

public class Hbm2DocExporterTask extends ExporterTask {

	public Hbm2DocExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public String getName() {
		return "hbm2doc (Generates html schema documentation)";
	}

	protected Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.DOC);
	}
}
