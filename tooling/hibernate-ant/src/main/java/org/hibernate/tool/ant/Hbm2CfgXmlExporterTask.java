/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.cfg.CfgXmlExporter;

public class Hbm2CfgXmlExporterTask extends ExporterTask {

	private boolean ejb3;

	public Hbm2CfgXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public void setEjb3(boolean ejb3) {
		this.ejb3 = ejb3;
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		CfgXmlExporter exporter = CfgXmlExporter.create(md);
		Properties props = new Properties();
		props.putAll(parent.getProperties());
		props.putAll(properties);
		props.setProperty("ejb3", "" + ejb3);
		File outputFile = new File(getDestdir(), "hibernate.cfg.xml");
		outputFile.getParentFile().mkdirs();
		try (Writer writer = new FileWriter(outputFile)) {
			exporter.export(writer, props);
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Failed to export hibernate.cfg.xml to "
					+ outputFile, e);
		}
	}

	public Exporter createExporter() {
		return null;
	}

	public String getName() {
		return "hbm2cfgxml (Generates hibernate.cfg.xml)";
	}

}
