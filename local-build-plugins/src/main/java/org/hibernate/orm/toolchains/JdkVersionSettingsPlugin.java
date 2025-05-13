/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.toolchains;

import org.gradle.StartParameter;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
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
		final JavaLanguageVersion baselineJdkVersion = getJavaLanguageVersion( settings, "orm.jdk.base" );
		final JavaLanguageVersion maxSupportedJdkVersion = getJavaLanguageVersion( settings, "orm.jdk.max" );

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
	private JavaLanguageVersion getJavaLanguageVersion(Settings settings, String name) {
		final StartParameter startParameter = settings.getStartParameter();
		final String fromSysProp = startParameter.getSystemPropertiesArgs().get( name );
		if ( fromSysProp != null && !fromSysProp.isEmpty() ) {
			return JavaLanguageVersion.of( fromSysProp );
		}

		final String fromProjProp = startParameter.getProjectProperties().get( name );
		if ( fromProjProp != null && !fromProjProp.isEmpty() ) {
			return JavaLanguageVersion.of( fromProjProp );
		}

		// This extracts info from gradle.properties
		final String fromGradleProp = settings.getProviders().gradleProperty( name ).getOrNull();
		if ( fromGradleProp != null && !fromGradleProp.isEmpty() ) {
			return JavaLanguageVersion.of( fromGradleProp );
		}

		return JavaLanguageVersion.of( JavaVersion.current().getMajorVersion() );
	}
}
