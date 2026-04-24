/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.dao.DaoExporter;

/**
 * @author Dennis Byrne
 */
public class Hbm2DAOExporterTask extends ExporterTask {

	boolean ejb3 = true;

	boolean jdk5 = true;

	public Hbm2DAOExporterTask(HibernateToolTask parent) {
		super(parent);
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
		DaoExporter.create(md, ejb3, "SessionFactory", tPath)
				.exportAll(getDestdir());
	}

	protected Exporter createExporter() {
		return null;
	}

	public String getName() {
		return "hbm2dao (Generates a set of DAOs)";
	}

}
