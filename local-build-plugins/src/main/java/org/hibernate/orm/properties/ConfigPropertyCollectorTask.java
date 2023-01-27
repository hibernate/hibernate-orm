/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

public class ConfigPropertyCollectorTask extends DefaultTask {

	private final Path javadocsLocation;
	private final Property<String> javadocsBaseLink;
	private final String anchor;
	private final String moduleName;
	private final ConfigPropertyHolder propertyHolder = new ConfigPropertyHolder();

	private final Path output;
	private final String fileName;


	@Inject
	public ConfigPropertyCollectorTask(Project project) {
		this.javadocsLocation = project.getBuildDir().toPath().resolve( "docs/javadoc" );
		this.javadocsBaseLink = project.getObjects().property( String.class );
		this.anchor = project.getName() + "-";
		this.moduleName = project.getDescription();
		this.output = project.getRootProject().project( ":documentation" ).getBuildDir().toPath()
				.resolve( "config-properties" );
		this.fileName = project.getName() + ".asciidoc";
	}

	@Internal
	public Property<String> getJavadocsBaseLink() {
		return javadocsBaseLink;
	}

	@TaskAction
	public void generateConfigProperties() {
		new ConfigurationPropertyCollector(
				propertyHolder, getLogger(), javadocsLocation, javadocsBaseLink.get(), anchor, moduleName
		).processClasses();
		try{
			Files.createDirectories( output );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to prepare output directory structure", e );
		}
		try ( Writer writer = new FileWriter( output.resolve( fileName ).toFile() ) ) {
			propertyHolder.write( new AsciiDocWriter( anchor, moduleName ), writer );
		}
		catch (IOException e) {
			throw new RuntimeException( "Failed to produce asciidoc output for collected properties", e );
		}
	}
}
