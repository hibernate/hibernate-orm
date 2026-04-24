/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.lint.HbmLintExporter;

public class HbmLintExporterTask extends ExporterTask {

	public HbmLintExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		String[] tPath = getTemplatePath().list();
		HbmLintExporter exporter = HbmLintExporter.create(md, tPath);
		exporter.export(getDestdir());
	}

	protected Exporter createExporter() {
		return null;
	}

	String getName() {
		return "hbmlint (scans mapping for errors)";
	}

}
