/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.build

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.internal.jvm.Jvm

/**
 * Defines a Gradle Project extension to hold information pertaining to the targeted Java version
 * for the project.  This information can then be used to configure tasks (JavaCompile, etc).
 *
 * @author Steve Ebersole
 */
class JavaTargetExtension {
	private final Project project

	JavaVersion version = JavaVersion.VERSION_1_6

	boolean shouldApplyTargetToCompile = true
	boolean shouldApplyTargetToTest = true

	JavaTargetExtension(Project project) {
		this.project = project
	}

	JavaVersion getVersion() {
		return version
	}

	void setVersion(Object version) {
		this.version = JavaVersion.toVersion( version );
		if ( this.version == null ) {
			this.version = JavaVersion.VERSION_1_6
		}
	}

	Jvm getJavaHome() {
		return javaHome
	}

	void setJavaHome(Object javaHome) {
		if ( javaHome == null ) {
			this.javaHome = null
		}
		else if ( javaHome instanceof Jvm ) {
			this.javaHome = javaHome as Jvm
		}
		else {
			final File specifiedJavaHome = project.file( javaHome );
			if ( specifiedJavaHome == null ) {
				throw new GradleException( "Could not resolve specified java home ${javaHome}" )
			}
			if ( !specifiedJavaHome.exists() ) {
				throw new GradleException( "Specified java home [${javaHome}] does not exist" )
			}
			if ( !specifiedJavaHome.isDirectory() ) {
				throw new GradleException( "Specified java home [${javaHome}] is not a directory" )
			}
			this.javaHome = Jvm.forHome( specifiedJavaHome ) as Jvm
		}
	}

	boolean getShouldApplyTargetToCompile() {
		return shouldApplyTargetToCompile
	}

	void setShouldApplyTargetToCompile(boolean shouldApplyTargetToCompile) {
		this.shouldApplyTargetToCompile = shouldApplyTargetToCompile
	}

	boolean getShouldApplyTargetToTest() {
		return shouldApplyTargetToTest
	}

	void setShouldApplyTargetToTest(boolean shouldApplyTargetToTest) {
		this.shouldApplyTargetToTest = shouldApplyTargetToTest
	}
}
