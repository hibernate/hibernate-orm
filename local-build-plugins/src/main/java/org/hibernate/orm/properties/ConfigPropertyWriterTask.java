/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.inject.Inject;

import org.hibernate.orm.properties.processor.AsciiDocWriter;
import org.hibernate.orm.properties.processor.ConfigPropertyHolder;
import org.hibernate.orm.properties.processor.ConfigurationProperty;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

/**
 * Task that writes two asciidoc files from the collected config properties. One is for public configurations, another - for SPI.
 */
public class ConfigPropertyWriterTask extends DefaultTask {

	private final Project project;
	private final ConfigPropertyHolder properties;
	private final String fileName = "configs";

	@Inject
	public ConfigPropertyWriterTask(Project project, ConfigPropertyHolder properties) {
		this.project = project;
		this.properties = properties;
	}

	@TaskAction
	public void writeProperties() {
		if ( properties.hasProperties() ) {
			writeProperties(
					fileName + ".asciidoc",
					new AsciiDocWriter()
			);
		}
	}

	private void writeProperties(String fileName, BiConsumer<Map<String, ConfigurationProperty>, Writer> transformer) {
		try ( Writer writer = new FileWriter( project.getBuildDir().toPath().resolve( fileName ).toFile() )
		) {
			properties.write( transformer, writer );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

}
