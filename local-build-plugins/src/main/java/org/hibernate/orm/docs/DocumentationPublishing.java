/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import org.hibernate.orm.ReleaseFamilyIdentifier;

/**
 * Gradle DSL extension for configuring documentation publishing
 *
 * @author Steve Ebersole
 */
public class DocumentationPublishing {
	public static final String DSL_NAME = "documentationPublishing";

	private final Project project;

	private final DirectoryProperty stagingDirectory;
	private final Property<String> docServerUrl;
	private final Property<String> docDescriptorServerUrl;

	private final RegularFileProperty updatedJsonFile;

	private final ReleaseFamilyIdentifier releaseFamilyIdentifier;

	@Inject
	public DocumentationPublishing(Project project) {
		this.project = project;

		stagingDirectory = project.getObjects()
				.directoryProperty()
				.convention( project.getLayout().getBuildDirectory().dir( "documentation" ) );
		docServerUrl = project.getObjects()
				.property( String.class )
				.convention( "filemgmt-prod-sync.jboss.org:/docs_htdocs/hibernate/orm" );

		docDescriptorServerUrl = project.getObjects()
				.property( String.class )
				.convention( "filemgmt-prod-sync.jboss.org:/docs_htdocs/hibernate" );


		updatedJsonFile = project.getObjects()
				.fileProperty()
				.convention( project.getLayout().getBuildDirectory().file( "doc-pub/orm.json" ) );

		releaseFamilyIdentifier = ReleaseFamilyIdentifier.parse( project.getVersion().toString() );
	}

	public ReleaseFamilyIdentifier getReleaseFamilyIdentifier() {
		return releaseFamilyIdentifier;
	}

	public Provider<RegularFile> getUpdatedJsonFile() {
		return updatedJsonFile;
	}

	public Property<String> getDocServerUrl() {
		return docServerUrl;
	}

	public Property<String> getDocDescriptorServerUrl() {
		return docDescriptorServerUrl;
	}

	public DirectoryProperty getStagingDirectory() {
		return stagingDirectory;
	}

	public void setUpdatedJsonFile(Object ref) {
		updatedJsonFile.fileValue( project.file( ref ) );
	}

	public void updatedJsonFile(Object ref) {
		updatedJsonFile.fileValue( project.file( ref ) );
	}
}
