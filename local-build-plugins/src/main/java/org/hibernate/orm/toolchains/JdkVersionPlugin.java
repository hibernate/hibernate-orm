/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.toolchains;

import javax.inject.Inject;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

import org.jetbrains.annotations.NotNull;

import static org.hibernate.orm.toolchains.JdkVersionConfig.MAIN_JDK_VERSION;
import static org.hibernate.orm.toolchains.JdkVersionConfig.TEST_JDK_VERSION;
import static org.hibernate.orm.toolchains.JdkVersionConfig.createVersionConfig;
import static org.hibernate.orm.toolchains.JdkVersionConfig.extractVersion;

/**
 * Gathers JDK versions of interest from settings and exposes them to later phases of the build
 * than {@link JdkVersionSettingsPlugin}.
 *
 * @see JdkVersionConfig
 * @see JdkVersionSettingsPlugin
 * @see JavaModulePlugin
 *
 * @author Steve Ebersole
 */
public class JdkVersionPlugin implements Plugin<Project> {
	private final JavaToolchainService toolchainService;

	@Inject
	public JdkVersionPlugin(JavaToolchainService toolchainService) {
		this.toolchainService = toolchainService;
	}

	@Override
	public void apply(Project project) {
		final JavaLanguageVersion explicitMainVersion = extractVersion( project, MAIN_JDK_VERSION );
		final JavaLanguageVersion explicitTestVersion = extractVersion( project, TEST_JDK_VERSION );

		final JavaLanguageVersion gradleJdkVersion = JavaLanguageVersion.of( JavaVersion.current().getMajorVersion() );
		final VersionCatalogsExtension versionCatalogs = project.getExtensions().getByType( VersionCatalogsExtension.class );
		final VersionCatalog jdkVersions = versionCatalogs.named( "jdks" );
		final JavaLanguageVersion baselineJdkVersion = getJavaLanguageVersion( jdkVersions, "baseline" );
		final JavaLanguageVersion maxSupportedJdkVersion = getJavaLanguageVersion( jdkVersions, "maxSupportedBytecode" );

		final JdkVersionConfig jdkVersionConfig = createVersionConfig(
				explicitMainVersion,
				explicitTestVersion,
				gradleJdkVersion,
				baselineJdkVersion,
				maxSupportedJdkVersion
		);

		project.getExtensions().add( JdkVersionConfig.DSL_NAME, jdkVersionConfig );
	}

	@NotNull
	private static JavaLanguageVersion getJavaLanguageVersion(VersionCatalog jdks, String entryName) {
		final VersionConstraint versionConstraint = jdks.findVersion( entryName ).orElseThrow();
		return JavaLanguageVersion.of( versionConstraint.getRequiredVersion() );
	}
}
