/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.cfg.CfgXmlExporter;

@DisableCachingByDefault(because = "Generates output from a live database connection")
public class GenerateCfgTask extends RevengTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating CFG exporter");
		MetadataDescriptor md = createJdbcDescriptor();
		File outputFolder = getOutputFolder();
		File outputFile = new File(outputFolder, "hibernate.cfg.xml");
		outputFile.getParentFile().mkdirs();
		Properties props = new Properties();
		props.putAll(getHibernateProperties());
		getLogger().lifecycle("Starting CFG export to directory: " + outputFolder + "...");
		try (Writer writer = new FileWriter(outputFile)) {
			CfgXmlExporter.create(md)
					.export(writer, props);
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Failed to export hibernate.cfg.xml to "
					+ outputFile, e);
		}
		getLogger().lifecycle("CFG export finished");
	}

}
