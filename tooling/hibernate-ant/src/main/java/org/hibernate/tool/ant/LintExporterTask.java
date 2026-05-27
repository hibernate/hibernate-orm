/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

public class LintExporterTask extends ExporterTask {

	public LintExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	protected Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.LINT);
	}


	String getName() {
		return "lint (scans mapping for errors)";
	}

}
