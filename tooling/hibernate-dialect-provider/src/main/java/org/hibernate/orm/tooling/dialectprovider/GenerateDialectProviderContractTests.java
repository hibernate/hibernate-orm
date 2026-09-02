/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.IOException;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.CacheableTask;
import org.hibernate.orm.tooling.dialectprovider.internal.ContractTestGenerator;

/// Generates the JUnit bridge which invokes provider-written Dialect contract
/// profiles through `hibernate-dialect-testkit`.
///
/// @author Steve Ebersole
/// @since 8.0
@CacheableTask
public abstract class GenerateDialectProviderContractTests extends DefaultTask {
	@Input
	public abstract ListProperty<String> getContractProfiles();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@TaskAction
	public void generate() {
		final List<String> profiles = getContractProfiles().get();
		try {
			ContractTestGenerator.generate( getOutputDirectory().get().getAsFile().toPath(), profiles );
		}
		catch (IllegalArgumentException | IOException e) {
			throw new GradleException( "Unable to generate Dialect-provider contract tests", e );
		}
	}
}
