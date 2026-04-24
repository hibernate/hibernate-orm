/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.hbm.HbmXmlExporter;

public class Hbm2HbmXmlExporterTask extends ExporterTask {

	public Hbm2HbmXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		String[] tPath = getTemplatePath().list();
		HbmXmlExporter.create(md, tPath)
				.exportAll(getDestdir());
	}

	protected Exporter createExporter() {
		return null;
	}

	public String getName() {
		return "hbm2hbmxml (Generates a set of hbm.xml files)";
	}
}
