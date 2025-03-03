/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	private final Property<String> docDescriptorUploadUrl;
	private final RegularFileProperty jsonFile;

	public PublishDescriptorTask() {
		setGroup( "documentation" );
		setDescription( "Publishes the documentation publication descriptor (JSON)" );

		projectVersion = getProject().provider( () -> getProject().getVersion() );
		docDescriptorUploadUrl = getProject().getObjects().property( String.class );
		jsonFile = getProject().getObjects().fileProperty();
	}

	@InputFile
	@SkipWhenEmpty
	public RegularFileProperty getJsonFile() {
		return jsonFile;
	}

	@Input
	public Property<String> getDocDescriptorUploadUrl() {
		return docDescriptorUploadUrl;
	}

	@Input
	public Provider<Object> getProjectVersion() {
		return projectVersion;
	}


	@TaskAction
	public void uploadDescriptor() {
		final String url = docDescriptorUploadUrl.get();
		RsyncHelper.rsync( jsonFile.get(), url, getProject() );
	}
}
