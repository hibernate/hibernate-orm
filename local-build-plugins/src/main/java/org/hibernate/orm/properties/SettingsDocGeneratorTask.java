/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.orm.ReleaseFamilyIdentifier;

import static org.hibernate.orm.properties.SettingsDocumentationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public class SettingsDocGeneratorTask extends DefaultTask {
	public static final String TASK_NAME = "generateSettingsDoc";

	private final DirectoryProperty javadocDirectory;
	private final Property<ReleaseFamilyIdentifier> releaseFamily;
	private final Property<String> publishedDocsUrl;

	private final NamedDomainObjectContainer<SettingsDocSection> sections;

	private final RegularFileProperty outputFile;

	@Inject
	public SettingsDocGeneratorTask(Project project) {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Collects descriptions of Hibernate configuration properties in preparation for inclusion in the User Guide" );

		javadocDirectory = project.getObjects().directoryProperty();
		javadocDirectory.convention( project.getLayout().getBuildDirectory().dir( "javadocs" ) );

		releaseFamily = project.getObjects().property( ReleaseFamilyIdentifier.class );
		releaseFamily.convention( project.provider( () -> ReleaseFamilyIdentifier.parse( project.getVersion().toString() ) ) );

		publishedDocsUrl = project.getObjects().property( String.class );
		publishedDocsUrl.convention( "https://docs.jboss.org/hibernate/orm" );

		sections = project.getObjects().domainObjectContainer( SettingsDocSection.class, SettingsDocSection::create );

		outputFile = project.getObjects().fileProperty();
		outputFile.convention( project.getLayout().getBuildDirectory().file( "asciidoc/fragments/config-settings.adoc" ) );
	}

	@InputDirectory
	@IgnoreEmptyDirectories
	public DirectoryProperty getJavadocDirectory() {
		return javadocDirectory;
	}

	@Input
	public Property<ReleaseFamilyIdentifier> getReleaseFamily() {
		return releaseFamily;
	}

//	@Nested
	@Internal
	public NamedDomainObjectContainer<SettingsDocSection> getSections() {
		return sections;
	}

	@OutputFile
	public RegularFileProperty getOutputFile() {
		return outputFile;
	}

	@TaskAction
	public void generateSettingsDocumentation() {
		final String publishedJavadocUrl = publishedDocsUrl.get()
				+ "/"
				+ releaseFamily.get().getFamilyVersion()
				+ "/javadocs/";

		AsciiDocWriter.writeToFile(
				SettingsCollector.collectSettingDescriptors(
						javadocDirectory.get(),
						sections.getAsMap(),
						publishedJavadocUrl
				),
				outputFile.get(),
				getProject()
		);
	}
}
