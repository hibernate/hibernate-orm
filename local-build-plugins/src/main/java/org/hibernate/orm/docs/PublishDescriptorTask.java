/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class PublishDescriptorTask extends DefaultTask {
	public static final String UPLOAD_DESC_TASK_NAME = "uploadDocumentationDescriptor";

	private final Provider<Object> projectVersion;
	private final Property<String> docServerUrl;
	private final RegularFileProperty jsonFile;

	public PublishDescriptorTask() {
		setGroup( "documentation" );
		setDescription( "Publishes the documentation publication descriptor (JSON)" );

		projectVersion = getProject().provider( () -> getProject().getVersion() );
		docServerUrl = getProject().getObjects().property( String.class );
		jsonFile = getProject().getObjects().fileProperty();
	}

	@InputFile
	@SkipWhenEmpty
	public RegularFileProperty getJsonFile() {
		return jsonFile;
	}

	@Input
	public Property<String> getDocServerUrl() {
		return docServerUrl;
	}

	@Input
	public Provider<Object> getProjectVersion() {
		return projectVersion;
	}


	@TaskAction
	public void uploadDescriptor() {
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + "_outdated-content/orm.json";

		RsyncHelper.rsync( jsonFile.get(), url, getProject() );
	}
}
