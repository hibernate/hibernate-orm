/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.doc.DocExporter;

public class Hbm2DocExporterTask extends ExporterTask {

	public Hbm2DocExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public String getName() {
		return "hbm2doc (Generates html schema documentation)";
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		String[] tPath = getTemplatePath().list();
		DocExporter.create(md, tPath)
				.export(getDestdir());
	}

	protected Exporter createExporter() {
		return null;
	}
}
