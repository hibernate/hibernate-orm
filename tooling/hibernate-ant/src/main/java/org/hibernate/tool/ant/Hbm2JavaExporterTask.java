/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

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

	protected Exporter configureExporter(Exporter exp) {
		super.configureExporter( exp );
		exp.getProperties().setProperty("ejb3", ""+ejb3);
		exp.getProperties().setProperty("jdk5", ""+jdk5);
		return exp;
	}

	protected Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.JAVA);
	}

	public String getName() {
		return "hbm2java (Generates a set of .java files)";
	}
}
