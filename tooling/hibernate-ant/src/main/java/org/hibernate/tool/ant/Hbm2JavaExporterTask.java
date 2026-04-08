/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ant;

import java.util.Properties;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.entity.EntityExporter;

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
