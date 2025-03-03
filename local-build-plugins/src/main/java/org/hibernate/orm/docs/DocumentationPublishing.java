/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public static final String RSYNC_SERVER = "filemgmt-prod-sync.jboss.org";
	public static final String SFTP_SERVER = "filemgmt-prod.jboss.org";

	public static final String DOC_SERVER_BASE_DIR = "/docs_htdocs/hibernate";

	public static final String DESCRIPTOR_FILE = "doc-pub/orm.json";

	private final Project project;

	private final DirectoryProperty stagingDirectory;

	private final Property<String> rsyncDocServer;
	private final Property<String> sftpDocServer;
	private final Property<String> serverBaseDir;

	private final RegularFileProperty updatedJsonFile;

	private final ReleaseFamilyIdentifier releaseFamilyIdentifier;

	@Inject
	public DocumentationPublishing(Project project) {
		this.project = project;

		stagingDirectory = project.getObjects()
				.directoryProperty()
				.convention( project.getLayout().getBuildDirectory().dir( "documentation" ) );


		rsyncDocServer = project.getObjects()
				.property( String.class )
				.convention( RSYNC_SERVER );

		sftpDocServer = project.getObjects()
				.property( String.class )
				.convention( SFTP_SERVER );

		serverBaseDir = project.getObjects()
				.property( String.class )
				.convention( DOC_SERVER_BASE_DIR );

		updatedJsonFile = project.getObjects()
				.fileProperty()
				.convention( project.getLayout().getBuildDirectory().file( DESCRIPTOR_FILE ) );

		releaseFamilyIdentifier = ReleaseFamilyIdentifier.parse( project.getVersion().toString() );
	}

	public ReleaseFamilyIdentifier getReleaseFamilyIdentifier() {
		return releaseFamilyIdentifier;
	}

	public Property<String> getRsyncDocServer() {
		return rsyncDocServer;
	}

	public Property<String> getSftpDocServer() {
		return sftpDocServer;
	}

	public Property<String> getServerBaseDir() {
		return serverBaseDir;
	}

	public DirectoryProperty getStagingDirectory() {
		return stagingDirectory;
	}

	public Provider<RegularFile> getUpdatedJsonFile() {
		return updatedJsonFile;
	}

	public void setUpdatedJsonFile(Object ref) {
		updatedJsonFile.fileValue( project.file( ref ) );
	}

	public void updatedJsonFile(Object ref) {
		updatedJsonFile.fileValue( project.file( ref ) );
	}
}
