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

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.doc.DocExporter;

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
