/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.toolchains;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.initialization.Settings;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNullElse;

/**
 * Describes the JDK versions of interest to the Hibernate build
 *
 * @author Steve Ebersole
 */
public class JdkVersionConfig {
	public static final String DSL_NAME = "jdkVersions";
	public static final String MAIN_JDK_VERSION = "main.jdk.version";
	public static final String TEST_JDK_VERSION = "test.jdk.version";

	private final boolean explicit;
	private final JavaLanguageVersion baseline;
	private final JavaLanguageVersion max;
	private final MainJdks main;
	private final TestJdks test;

	public JdkVersionConfig(
			boolean explicit,
			JavaLanguageVersion baseline,
			JavaLanguageVersion max,
			JavaLanguageVersion mainCompileVersion,
			JavaLanguageVersion mainReleaseVersion,
			JavaLanguageVersion testCompileVersion,
			JavaLanguageVersion testReleaseVersion,
			JavaLanguageVersion testLauncherVersion) {
		this.explicit = explicit;
		this.baseline = baseline;
		this.max = max;
		this.main = new MainJdks( mainCompileVersion, mainReleaseVersion );
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

	public JavaLanguageVersion getMainCompileVersion() {
		return main.getCompile();
	}

	public JavaLanguageVersion getMainReleaseVersion() {
		return main.getRelease();
	}

	public JavaLanguageVersion getTestCompileVersion() {
		return test.getCompile();
	}

	public JavaLanguageVersion getTestReleaseVersion() {
		return test.getRelease();
	}

	public JavaLanguageVersion getTestLauncherVersion() {
		return test.getLauncher();
	}

	public Set<JavaLanguageVersion> getAllVersions() {
		final HashSet<JavaLanguageVersion> versions = new HashSet<>();
		versions.add( getMainCompileVersion() );
		versions.add( getMainReleaseVersion() );
		versions.add( getTestCompileVersion() );
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
			JavaLanguageVersion maxSupportedJdkVersion) {
		final boolean explicitlyConfigured = explicitMainVersion != null || explicitTestVersion != null;

		final JavaLanguageVersion mainCompileVersion;
		final JavaLanguageVersion mainReleaseVersion;
		final JavaLanguageVersion testCompileVersion;
		final JavaLanguageVersion testReleaseVersion;
		final JavaLanguageVersion testLauncherVersion;

		if ( explicitlyConfigured ) {
			mainCompileVersion = requireNonNullElse( explicitMainVersion, baselineJdkVersion );
			testCompileVersion = requireNonNullElse( explicitTestVersion, baselineJdkVersion );
			mainReleaseVersion = baselineJdkVersion;

			if ( testCompileVersion.asInt() > maxSupportedJdkVersion.asInt() ) {
				System.out.println(
						"[WARN] Gradle does not support bytecode version '" + testCompileVersion + "'."
								+ " Forcing test bytecode to version " + maxSupportedJdkVersion + "."
				);
				testReleaseVersion = maxSupportedJdkVersion;
			}
			else {
				testReleaseVersion = testCompileVersion;
			}

			testLauncherVersion = testCompileVersion;

			return new JdkVersionConfig(
					true,
					baselineJdkVersion,
					maxSupportedJdkVersion,
					mainCompileVersion,
					mainReleaseVersion,
					testCompileVersion,
					testReleaseVersion,
					testLauncherVersion
			);
		}
		else {
			// Not testing a particular JDK version: we will use the same JDK used to run Gradle.
			// We disable toolchains for convenience, so that anyone can just run the build with their own JDK
			// without any additional options and without downloading the whole JDK.
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
		private final JavaLanguageVersion compileVersion;
		private final JavaLanguageVersion releaseVersion;

		public MainJdks(JavaLanguageVersion compileVersion, JavaLanguageVersion releaseVersion) {
			this.compileVersion = compileVersion;
			this.releaseVersion = releaseVersion;
		}

		@Override
		public JavaLanguageVersion getCompile() {
			return compileVersion;
		}

		public JavaLanguageVersion getCompiler() {
			return compileVersion;
		}

		@Override
		public JavaLanguageVersion getRelease() {
			return releaseVersion;
		}

		@Override
		public String toString() {
			return "[compile: " + compileVersion + ", release:" + releaseVersion + "]";
		}
	}

	public static class TestJdks implements JdkVersionCombo {
		private final JavaLanguageVersion compileVersion;
		private final JavaLanguageVersion releaseVersion;
		private final JavaLanguageVersion launcherVersion;

		public TestJdks(
				JavaLanguageVersion compileVersion,
				JavaLanguageVersion releaseVersion,
				JavaLanguageVersion launcherVersion) {
			this.compileVersion = compileVersion;
			this.releaseVersion = releaseVersion;
			this.launcherVersion = launcherVersion;
		}

		public JavaLanguageVersion getCompiler() {
			return compileVersion;
		}

		@Override
		public JavaLanguageVersion getCompile() {
			return compileVersion;
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
			return "[compile: " + compileVersion + ", release:" + releaseVersion + ", launcher: " + launcherVersion + "]";
		}
	}

	public interface  JdkVersionCombo {
		JavaLanguageVersion getCompile();
		JavaLanguageVersion getRelease();
	}
}
