/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta;

import java.io.File;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.jakarta.JakartaPlugin.JAKARTA;

/**
 * @author Steve Ebersole
 */
@CacheableTask
public abstract class JakartaDirectoryTransformation extends DefaultTask {
	private final DirectoryProperty sourceDirectory;
	private final DirectoryProperty targetDirectory;

	@Inject
	public JakartaDirectoryTransformation(ObjectFactory objectFactory) {
		sourceDirectory = objectFactory.directoryProperty();
		targetDirectory = objectFactory.directoryProperty();

		setGroup( JAKARTA );
	}

	@InputDirectory
	@PathSensitive( PathSensitivity.RELATIVE )
	public DirectoryProperty getSourceDirectory() {
		return sourceDirectory;
	}

	@OutputDirectory
	public DirectoryProperty getTargetDirectory() {
		return targetDirectory;
	}

	@TaskAction
	void transform() {
		final File sourceDirAsFile = sourceDirectory.get().getAsFile();
		final File targetDirAsFile = targetDirectory.get().getAsFile();

		// If the target directory already exists, the transformer tool will
		// skip the transformation - even if the directory is empty.
		// Gradle is nice enough to make sure that directory exists, but
		// unfortunately that "confuses" the transformer tool.
		//
		// For now, delete the dir before executing the transformer.
		//
		// NOTE : Gradle has already done its up-to-date checks and our task
		// is actually executing at this point, so deleting the directory will
		// have no effect on the incremental build

		targetDirAsFile.delete();

		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.classpath( getProject().getConfigurations().getByName( "jakartaeeTransformTool" ) );
					javaExecSpec.setMain( "org.eclipse.transformer.jakarta.JakartaTransformer" );
					javaExecSpec.args(
							sourceDirAsFile.getAbsolutePath(),
							targetDirAsFile.getAbsolutePath(),
							"-q",
							"-tr", getProject().getRootProject().file( "rules/jakarta-renames.properties" ).getAbsolutePath(),
							"-tv", getProject().getRootProject().file( "rules/jakarta-versions.properties" ).getAbsolutePath(),
							"-td", getProject().getRootProject().file( "rules/jakarta-direct.properties" ).getAbsolutePath()
					);
				}
		);
	}
}
