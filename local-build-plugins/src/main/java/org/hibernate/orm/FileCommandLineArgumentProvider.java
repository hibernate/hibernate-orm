/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm;

import java.io.File;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * @author Steve Ebersole
 */
public class FileCommandLineArgumentProvider implements CommandLineArgumentProvider {
	private final String argumentName;

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	RegularFileProperty path;

	public FileCommandLineArgumentProvider(String argumentName, Project project) {
		this.argumentName = argumentName;
		path = project.getObjects().fileProperty();
	}

	public FileCommandLineArgumentProvider(String argumentName, RegularFile path, Project project) {
		this( argumentName, project );
		this.path.set( path );
	}

	@Override
	public Iterable<String> asArguments() {
		final File pathAsFile = path.get().getAsFile();
		return Collections.singleton(
				String.format(
						"-D%s=%s",
						argumentName,
						pathAsFile.getAbsolutePath()
				)
		);
	}
}
