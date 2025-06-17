/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.toolchains;

import java.util.HashSet;
import java.util.Set;

import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNullElse;

/**
 * Describes the JDK versions of interest to the Hibernate build
 *
 * @see JdkVersionSettingsPlugin
 * @see JdkVersionPlugin
 * @see JavaModulePlugin
 *
 * @author Steve Ebersole
 */
public class JdkVersionConfig {
	public static final String DSL_NAME = "jdkVersions";
	public static final String MAIN_JDK_VERSION = "main.jdk.version";
	public static final String TEST_JDK_VERSION = "test.jdk.version";

	private final boolean explicit;
	private final JavaLanguageVersion baseline;
	private final JavaLanguageVersion min;
	private final JavaLanguageVersion max;
	private final MainJdks main;
	private final TestJdks test;

	public JdkVersionConfig(
			boolean explicit,
			JavaLanguageVersion baseline,
			JavaLanguageVersion min,
			JavaLanguageVersion max,
			JavaLanguageVersion mainCompilerVersion,
			JavaLanguageVersion mainReleaseVersion,
			JavaLanguageVersion testCompileVersion,
			JavaLanguageVersion testReleaseVersion,
			JavaLanguageVersion testLauncherVersion) {
		this.explicit = explicit;
		this.baseline = baseline;
		this.min = min;
		this.max = max;
		this.main = new MainJdks( mainCompilerVersion, mainReleaseVersion );
		this.test = new TestJdks( testCompileVersion, testReleaseVersion, testLauncherVersion );
	}

	public boolean isExplicitlyConfigured() {
		return explicit;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public JavaLanguageVersion getBaseline() {
		return baseline;
	}

	public String getBaselineStr() {
		return getBaseline().toString();
	}

	public JavaLanguageVersion getBaselineVersion() {
		return getBaseline();
	}

	public JavaLanguageVersion getMin() {
		return min;
	}

	public String getMinStr() {
		return getMin().toString();
	}

	public JavaLanguageVersion getMinVersion() {
		return getMin();
	}

	public JavaLanguageVersion getMax() {
		return max;
	}

	public String getMaxStr() {
		return getMax().toString();
	}

	public JavaLanguageVersion getMaxVersion() {
		return getMax();
	}

	public MainJdks getMain() {
		return main;
	}

	public TestJdks getTest() {
		return test;
	}

	public JavaLanguageVersion getMainCompilerVersion() {
		return main.getCompiler();
	}

	public JavaLanguageVersion getMainReleaseVersion() {
		return main.getRelease();
	}

	public JavaLanguageVersion getTestCompilerVersion() {
		return test.getCompiler();
	}

	public JavaLanguageVersion getTestReleaseVersion() {
		return test.getRelease();
	}

	public JavaLanguageVersion getTestLauncherVersion() {
		return test.getLauncher();
	}

	public Set<JavaLanguageVersion> getAllVersions() {
		final HashSet<JavaLanguageVersion> versions = new HashSet<>();
		versions.add( getMainCompilerVersion() );
		versions.add( getMainReleaseVersion() );
		versions.add( getTestCompilerVersion() );
		versions.add( getTestReleaseVersion() );
		versions.add( getTestLauncherVersion() );
		return versions;
	}

	@NotNull
	public static JdkVersionConfig createVersionConfig(
			JavaLanguageVersion explicitMainVersion,
			JavaLanguageVersion explicitTestVersion,
			JavaLanguageVersion gradleJdkVersion,
			JavaLanguageVersion baselineJdkVersion,
			JavaLanguageVersion minSupportedJdkVersion,
			JavaLanguageVersion maxSupportedJdkVersion) {
		final boolean explicitlyConfigured = explicitMainVersion != null || explicitTestVersion != null;

		final JavaLanguageVersion mainCompilerVersion;
		final JavaLanguageVersion mainReleaseVersion;
		final JavaLanguageVersion testCompilerVersion;
		JavaLanguageVersion testReleaseVersion;
		final JavaLanguageVersion testLauncherVersion;

		if ( explicitlyConfigured ) {
			mainCompilerVersion = requireNonNullElse( explicitMainVersion, minSupportedJdkVersion );
			testCompilerVersion = requireNonNullElse( explicitTestVersion, minSupportedJdkVersion );
			mainReleaseVersion = baselineJdkVersion;

			testReleaseVersion = requireNonNullElse( explicitTestVersion, mainReleaseVersion );
			if ( testReleaseVersion.asInt() > maxSupportedJdkVersion.asInt() ) {
				System.out.println(
						"[WARN] Gradle does not support bytecode version '" + testReleaseVersion + "'."
								+ " Forcing test bytecode to version " + maxSupportedJdkVersion + "."
				);
				testReleaseVersion = maxSupportedJdkVersion;
			}

			// This must not be downgraded like we do for the "release version",
			// first because we don't need to,
			// second because we don't necessarily have a lower version of the JDK available on the machine.
			testLauncherVersion = testCompilerVersion;

			return new JdkVersionConfig(
					true,
					baselineJdkVersion,
					minSupportedJdkVersion,
					maxSupportedJdkVersion,
					mainCompilerVersion,
					mainReleaseVersion,
					testCompilerVersion,
					testReleaseVersion,
					testLauncherVersion
			);
		}
		else {
			// Not testing a particular JDK version: we will use the same JDK used to run Gradle.
			// We disable toolchains for convenience, so that anyone can just run the build with their own JDK
			// without any additional options and without downloading the whole JDK.

			if ( gradleJdkVersion.asInt() < minSupportedJdkVersion.asInt() ) {
				throw new GradleException("This build requires at least JDK " + minSupportedJdkVersion + ", but you are using JDK " + gradleJdkVersion.asInt());
			}

			if ( gradleJdkVersion.asInt() > maxSupportedJdkVersion.asInt() ) {
				System.out.println(
						"[WARN] Gradle does not support this JDK, because it is too recent; build is likely to fail."
								+ " To avoid failures, you should use an older Java version when running Gradle, and rely on toolchains."
								+ " To that end, specify the version of Java you want to run tests with using property 'test.jdk.version',"
								+ " and specify the path to JDK8 *and* a JDK of the test version using property 'org.gradle.java.installations.paths'."
								+ " Example:"
								+ "  ./gradlew build -Ptest.jdk.version=15 -Porg.gradle.java.installations.paths=$SDKMAN_CANDIDATES_DIR/java/15.0.1-open,$SDKMAN_CANDIDATES_DIR/java/8"
				);
			}

			return new JdkVersionConfig(
					false,
					baselineJdkVersion,
					minSupportedJdkVersion,
					maxSupportedJdkVersion,
					gradleJdkVersion,
					baselineJdkVersion,
					gradleJdkVersion,
					baselineJdkVersion,
					gradleJdkVersion
			);
		}
	}

	public static JavaLanguageVersion extractVersion(Settings settings, String propertyName) {
		final StartParameter startParameters = settings.getGradle().getStartParameter();
		final String projectProp = startParameters.getProjectProperties().get( propertyName );
		if ( projectProp != null ) {
			return JavaLanguageVersion.of( projectProp );
		}

		final String sysProp = startParameters.getSystemPropertiesArgs().get( propertyName );
		if ( sysProp != null ) {
			return JavaLanguageVersion.of( sysProp );
		}

		return null;
	}

	public static JavaLanguageVersion extractVersion(Project project, String propertyName) {
		final Object projectProp = project.getProperties().get( propertyName );
		if ( projectProp != null ) {
			return JavaLanguageVersion.of( projectProp.toString() );
		}

		final Object sysProp = System.getProperties().get( propertyName );
		if ( sysProp != null ) {
			return JavaLanguageVersion.of( sysProp.toString() );
		}

		return null;
	}

	public static class MainJdks implements JdkVersionCombo {
		private final JavaLanguageVersion compilerVersion;
		private final JavaLanguageVersion releaseVersion;

		public MainJdks(JavaLanguageVersion compilerVersion, JavaLanguageVersion releaseVersion) {
			this.compilerVersion = compilerVersion;
			this.releaseVersion = releaseVersion;
		}

		public JavaLanguageVersion getCompiler() {
			return compilerVersion;
		}

		@Override
		public JavaLanguageVersion getRelease() {
			return releaseVersion;
		}

		@Override
		public String toString() {
			return "[compiler: " + compilerVersion + ", release:" + releaseVersion + "]";
		}
	}

	public static class TestJdks implements JdkVersionCombo {
		private final JavaLanguageVersion compilerVersion;
		private final JavaLanguageVersion releaseVersion;
		private final JavaLanguageVersion launcherVersion;

		public TestJdks(
				JavaLanguageVersion compilerVersion,
				JavaLanguageVersion releaseVersion,
				JavaLanguageVersion launcherVersion) {
			this.compilerVersion = compilerVersion;
			this.releaseVersion = releaseVersion;
			this.launcherVersion = launcherVersion;
		}

		@Override
		public JavaLanguageVersion getCompiler() {
			return compilerVersion;
		}

		@Override
		public JavaLanguageVersion getRelease() {
			return releaseVersion;
		}

		public JavaLanguageVersion getLauncher() {
			return launcherVersion;
		}

		@Override
		public String toString() {
			return "[compiler: " + compilerVersion + ", release:" + releaseVersion + ", launcher: " + launcherVersion + "]";
		}
	}

	public interface  JdkVersionCombo {
		JavaLanguageVersion getCompiler();
		JavaLanguageVersion getRelease();
	}
}
