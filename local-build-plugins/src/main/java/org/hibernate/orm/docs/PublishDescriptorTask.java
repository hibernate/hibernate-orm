/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class PublishDescriptorTask extends DefaultTask {
	private final Provider<String> docServerUrl;
	private final Provider<RegularFile> jsonFile;

	@Inject
	public PublishDescriptorTask(DocumentationPublishing config) {
		setGroup( "Release" );
		setDescription( "Publishes the documentation publication descriptor (JSON)" );

		getInputs().property( "hibernate-version", getProject().getVersion() );

		docServerUrl = config.getDocDescriptorServerUrl();
		jsonFile = config.getUpdatedJsonFile();
	}

	@InputFile
	@SkipWhenEmpty
	public Provider<RegularFile> getJsonFile() {
		return jsonFile;
	}

	@Input
	public Provider<String> getDocServerUrl() {
		return docServerUrl;
	}

	@TaskAction
	public void uploadDescriptor() {
		final String base = docServerUrl.get();
		final String normalizedBase = base.endsWith( "/" ) ? base : base + "/";
		final String url = normalizedBase + "_outdated-content/orm.json";

		RsyncHelper.rsync( jsonFile.get(), url, getProject() );
	}
}
