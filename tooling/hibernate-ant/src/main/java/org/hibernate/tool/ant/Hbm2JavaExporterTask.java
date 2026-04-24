/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import java.util.Properties;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter;

/**
 * @author max
 *
 */
public class Hbm2JavaExporterTask extends ExporterTask {

	boolean ejb3 = true;

	boolean jdk5 = true;

	public Hbm2JavaExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	public void setEjb3(boolean b) {
		ejb3 = b;
	}

	public void setJdk5(boolean b) {
		jdk5 = b;
	}

	@Override
	public void execute() {
		MetadataDescriptor md = parent.getMetadataDescriptor();
		String[] tPath = getTemplatePath().list();
		EntityExporter exporter = EntityExporter.create(md, ejb3, jdk5, tPath);
		Properties props = new Properties();
		props.putAll(parent.getProperties());
		props.putAll(properties);
		exporter.setProperties(props);
		exporter.exportAll(getDestdir());
	}

	protected Exporter createExporter() {
		return null;
	}

	public String getName() {
		return "hbm2java (Generates a set of .java files)";
	}
}
