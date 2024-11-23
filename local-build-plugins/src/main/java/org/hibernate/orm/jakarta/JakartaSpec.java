/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.jakarta;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;

/**
 * Gradle DSL extension for configuring the Jakartafying transformations
 * needed for a project.  Mainly used to trigger different conventions depending
 * on whether the "source" is a local project or an external module
 *
 * NOTE : abstract to allow Gradle to "weave in" behavior (like being an extension container, etc)
 *
 * @author Steve Ebersole
 */
public abstract class JakartaSpec {
	public static final String REGISTRATION_NAME = "jakarta";

	private final Project jakartaProject;

	private Dependency sourceDependency;

	@Inject
	public JakartaSpec(Project jakartaProject) {
		// `jakartaProject` is the project where this plugin gets applied - the `-jakarta` one
		this.jakartaProject = jakartaProject;
	}

	public void source(Object dependencyNotation) {
		if ( sourceDependency != null ) {
			throw new IllegalStateException( "Source Dependency already specified" );
		}

		sourceDependency = jakartaProject.getDependencies().create( dependencyNotation );
	}

	public Dependency getSourceDependency() {
		return sourceDependency;
	}
}
