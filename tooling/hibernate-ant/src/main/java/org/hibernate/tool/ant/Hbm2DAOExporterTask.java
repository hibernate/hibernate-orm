/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

/**
 * @author Dennis Byrne
 */
public class Hbm2DAOExporterTask extends Hbm2JavaExporterTask {

	public Hbm2DAOExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	protected Exporter createExporter() {
		Exporter result = ExporterFactory.createExporter(ExporterType.DAO);
		result.getProperties().putAll(parent.getProperties());
		result.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, parent.getMetadataDescriptor());
		result.getProperties().put(ExporterConstants.DESTINATION_FOLDER, getDestdir());
		return result;
	}

	public String getName() {
		return "hbm2dao (Generates a set of DAOs)";
	}

}
