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

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.exporter.cfg.CfgXmlExporter;

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
		} catch (Exception e) {
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
