/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Validates exact Core and test-kit alignment before provider contract tests
/// execute.
///
/// @author Steve Ebersole
@DisableCachingByDefault(because = "Performs no work after validating resolved dependency identities")
public abstract class ValidateProviderTestRuntime extends DefaultTask {
	@Input
	public abstract Property<String> getConfiguredCoreVersion();

	@Input
	public abstract Property<String> getResolvedCoreVersion();

	@Input
	public abstract Property<String> getPluginVersion();

	@Input
	public abstract ListProperty<String> getContractProfiles();

	@Input
	public abstract ListProperty<String> getResolvedTestKitVersions();

	@TaskAction
	public void validateRuntime() {
		final String configured = getConfiguredCoreVersion().get();
		final String resolved = getResolvedCoreVersion().get();
		if ( !configured.equals( resolved ) ) {
			throw new GradleException(
					"Configured Hibernate ORM version " + configured
							+ " does not agree with resolved hibernate-core " + resolved
			);
		}
		HibernateVersions.verifyFamily( getPluginVersion().get(), resolved );
		if ( getContractProfiles().get().isEmpty() ) {
			return;
		}
		final List<String> testKitVersions = getResolvedTestKitVersions().get().stream().distinct().toList();
		if ( !testKitVersions.equals( List.of( resolved ) ) ) {
			throw new GradleException(
					"Dialect contract profiles require org.hibernate.orm:hibernate-dialect-testkit:" + resolved
							+ " but resolved versions were " + testKitVersions
			);
		}
	}
}
