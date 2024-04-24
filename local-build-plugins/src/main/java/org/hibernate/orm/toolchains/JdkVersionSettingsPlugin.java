/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.toolchains;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import org.jetbrains.annotations.NotNull;

import static org.hibernate.orm.toolchains.JdkVersionConfig.MAIN_JDK_VERSION;
import static org.hibernate.orm.toolchains.JdkVersionConfig.TEST_JDK_VERSION;
import static org.hibernate.orm.toolchains.JdkVersionConfig.createVersionConfig;
import static org.hibernate.orm.toolchains.JdkVersionConfig.extractVersion;

/**
 * @author Steve Ebersole
 */
public class JdkVersionSettingsPlugin implements Plugin<Settings> {
	@Override
	public void apply(Settings settings) {
		final JavaLanguageVersion explicitMainVersion = extractVersion( settings, MAIN_JDK_VERSION );
		final JavaLanguageVersion explicitTestVersion = extractVersion( settings, TEST_JDK_VERSION );

		final JavaLanguageVersion gradleJdkVersion = JavaLanguageVersion.of( JavaVersion.current().getMajorVersion() );
		final JavaLanguageVersion baselineJdkVersion;
		final JavaLanguageVersion maxSupportedJdkVersion;
//		final VersionCatalogsExtension versionCatalogs = settings.getExtensions().getByType( VersionCatalogsExtension.class );
//		final VersionCatalog jdkVersions = versionCatalogs.named( "jdks" );
//		baselineJdkVersion = getJavaLanguageVersion( jdkVersions, "baseline" );
//		maxSupportedJdkVersion = getJavaLanguageVersion( jdkVersions, "maxSupportedBytecode" );
		baselineJdkVersion = JavaLanguageVersion.of( "11" );
		maxSupportedJdkVersion = JavaLanguageVersion.of( "17" );

		final JdkVersionConfig jdkVersionConfig = createVersionConfig(
				explicitMainVersion,
				explicitTestVersion,
				gradleJdkVersion,
				baselineJdkVersion,
				maxSupportedJdkVersion
		);

		settings.getGradle().getExtensions().add( JdkVersionConfig.DSL_NAME, jdkVersionConfig );
		settings.getExtensions().add( JdkVersionConfig.DSL_NAME, jdkVersionConfig );
		JdkVersionsLogging.logVersions( jdkVersionConfig );
	}

	@NotNull
	private static JavaLanguageVersion getJavaLanguageVersion(VersionCatalog jdks, String entryName) {
		final VersionConstraint versionConstraint = jdks.findVersion( entryName ).orElseThrow();
		return JavaLanguageVersion.of( versionConstraint.getRequiredVersion() );
	}
}
